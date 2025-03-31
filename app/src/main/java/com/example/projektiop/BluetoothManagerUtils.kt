package com.example.projektiop // Upewnij się, że pakiet jest poprawny

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent // Potrzebne do otwierania ustawień
import android.content.pm.PackageManager
import android.location.LocationManager // Potrzebne do sprawdzania lokalizacji
import android.os.Build
import android.os.ParcelUuid
import android.provider.Settings // Potrzebne do otwierania ustawień lokalizacji
import android.util.Log
import android.widget.Toast // Dodano import dla Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat // Helper do sprawdzania lokalizacji
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.Charset
import java.util.UUID

// --- WAŻNE: Unikalny UUID dla twojej usługi - MUSI być taki sam w aplikacji skanującej i rozgłaszającej ---
// Możesz wygenerować własny np. za pomocą `UUID.randomUUID().toString()` i zastąpić ten poniżej
private val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Użyj własnego stałego UUID

// Tagi do filtrowania logów w Logcat
private const val TAG_SCAN = "BLE_SCAN_DEBUG"
private const val TAG_ADVERTISE = "BLE_ADVERTISE_DEBUG"
private const val TAG_LOCATION = "BLE_LOCATION_CHECK"
private const val TAG_PERMISSIONS = "BLE_PERMISSIONS" // Tag dla logów uprawnień

class BluetoothManagerUtils(
    private val context: Context,
    private val ownUserId: String // Przekazujemy ID użytkownika do rozgłaszania
) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // --- Stany obserwowane przez UI ---
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    // Przechowuje *ostatni* komunikat statusu (np. ostatnie znalezione ID)
    private val _foundDeviceStatus = MutableStateFlow("Status: Oczekuje...")
    val foundDeviceStatus = _foundDeviceStatus.asStateFlow()

    // --- Zbiór do przechowywania unikalnych ID znalezionych urządzeń podczas jednego skanowania ---
    private val foundDeviceIds = mutableSetOf<String>()

    // --- Skanowanie ---
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission") // Uprawnienia sprawdzane przed startem
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                val deviceAddress = scanResult.device?.address ?: "Brak adresu" // Bezpieczne odwołanie
                val record = scanResult.scanRecord ?: return@let // Potrzebujemy rekordu
                val serviceUuids = record.serviceUuids
                val targetParcelUuid = ParcelUuid(SERVICE_UUID)

                // Sprawdzamy UUID usługi
                if (serviceUuids?.contains(targetParcelUuid) == true) {
                    // Próbujemy odczytać dane usługi
                    val serviceData = record.getServiceData(targetParcelUuid)
                    if (serviceData != null) {
                        try {
                            val foundUserId = String(serviceData, Charset.forName("UTF-8")).trim() // Dodano trim()

                            // Sprawdzamy, czy ID jest niepuste i czy jest nowe
                            if (foundUserId.isNotEmpty() && foundDeviceIds.add(foundUserId)) {
                                // ID jest nowe, logujemy i aktualizujemy status
                                Log.i(TAG_SCAN, ">>> NOWE URZĄDZENIE: Znaleziono ID: $foundUserId (MAC: $deviceAddress) <<<")
                                _foundDeviceStatus.value = "Status: Znaleziono $foundUserId"
                                // Można dodać więcej logiki, np. zbierać wszystkie ID i pokazywać listę
                                // _foundDeviceStatus.value = "Status: Znaleziono (${foundDeviceIds.size}): ${foundDeviceIds.joinToString()}"
                            } else if (foundUserId.isNotEmpty()) {
                                // ID już znane w tej sesji skanowania
                                Log.v(TAG_SCAN, "onScanResult: Ponownie wykryto urządzenie z ID: $foundUserId (MAC: $deviceAddress)")
                            } else {
                                // Otrzymano puste dane
                                Log.w(TAG_SCAN, "onScanResult: Otrzymano puste ID od urządzenia $deviceAddress")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG_SCAN, "Błąd dekodowania danych usługi dla $deviceAddress", e)
                        }
                    } else {
                        // UUID pasuje, ale brak danych (ServiceData)
                        Log.w(TAG_SCAN, "onScanResult: Urządzenie $deviceAddress ma pasujące UUID, ale brak ServiceData.")
                    }
                } else {
                    // UUID nie pasuje (inne urządzenie BLE) - logujemy tylko na poziomie Verbose
                    Log.v(TAG_SCAN, "onScanResult: Zignorowano urządzenie $deviceAddress (nie pasuje UUID $SERVICE_UUID)")
                }
            } ?: run {
                Log.d(TAG_SCAN, "onScanResult: Otrzymano pusty wynik (result == null)")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG_SCAN, "Skanowanie nie powiodło się, kod błędu: $errorCode")
            _isScanning.value = false
            val errorText = when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan Failed: Already Started"
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Scan Failed: App Registration Failed (Check Manifest?)"
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Scan Failed: Internal Error"
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan Failed: Feature Unsupported (BLE Scan not supported?)"
                // Można dodać kody z API 31+ jeśli targetSDK >= 31
                // ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Scan Failed: Out of Hardware Resources"
                // ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "Scan Failed: Scanning Too Frequently"
                else -> "Scan Failed: Unknown error code $errorCode"
            }
            _foundDeviceStatus.value = "Status: Błąd skan. ($errorCode)"
            Log.e(TAG_SCAN, errorText)
        }
    }

    // --- Rozgłaszanie ---
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.i(TAG_ADVERTISE, ">>> Rozgłaszanie rozpoczęte pomyślnie (ID: $ownUserId, UUID: $SERVICE_UUID) <<<")
            _isAdvertising.value = true
            // Można zaktualizować status, np.
            // if (!_isScanning.value) { _foundDeviceStatus.value = "Status: Rozgłaszanie aktywne" }
        }
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG_ADVERTISE, "Rozgłaszanie nie powiodło się, kod błędu: $errorCode")
            _isAdvertising.value = false
            val errorText = when (errorCode) {
                AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertise Failed: Data Too Large (Check ID length and UUID)"
                AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Advertise Failed: Too Many Advertisers (System limit reached)"
                AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Advertise Failed: Already Started"
                AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Advertise Failed: Internal Error"
                AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertise Failed: Feature Unsupported (BLE Advertise not supported?)"
                else -> "Advertise Failed: Unknown error code $errorCode"
            }
            _foundDeviceStatus.value = "Status: Błąd rozgł. ($errorCode)"
            Log.e(TAG_ADVERTISE, errorText)
        }
    }

    // --- Publiczne metody kontrolne ---

    @SuppressLint("MissingPermission")
    fun startScan() {
        Log.d(TAG_SCAN, "Wywołano startScan()")

        // 1. Sprawdzenie uprawnień
        if (!hasPermissions(getRequiredPermissionsScan())) {
            Log.w(TAG_PERMISSIONS, "Brak uprawnień do skanowania.")
            _foundDeviceStatus.value = "Status: Brak uprawnień skan."
            return
        }

        // 2. Sprawdzenie, czy Bluetooth jest włączony
        if (bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG_SCAN, "Bluetooth jest wyłączony.")
            _foundDeviceStatus.value = "Status: Włącz Bluetooth"
            // Można tu dodać kod do żądania włączenia Bluetooth
            // context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        // 3. Sprawdzenie, czy Lokalizacja systemowa jest włączona
        if (!isLocationEnabled()) {
            Log.w(TAG_LOCATION, "Usługi lokalizacyjne systemu są wyłączone. Skanowanie BLE wymaga włączonej lokalizacji.")
            _foundDeviceStatus.value = "Status: Włącz Lokalizację"
            // Informujemy użytkownika, ale pozwalamy na próbę startu - system może sam zablokować
            // Jeśli chcesz całkowicie zablokować, dodaj: return
        } else {
            Log.d(TAG_LOCATION, "Usługi lokalizacyjne systemu są włączone.")
        }

        // 4. Sprawdzenie, czy już nie skanuje
        if (_isScanning.value) {
            Log.d(TAG_SCAN, "Skanowanie już aktywne.")
            return
        }

        // 5. Inicjalizacja skanera
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.e(TAG_SCAN, "Nie można uzyskać dostępu do skanera BLE (czy urządzenie wspiera BLE?).")
            _foundDeviceStatus.value = "Status: Błąd inicj. skanera"
            return
        }

        // 6. Wyczyść listę poprzednio znalezionych urządzeń przed nowym skanowaniem
        foundDeviceIds.clear()
        Log.d(TAG_SCAN, "Wyczyszczono zbiór znalezionych ID.")

        // 7. Ustawienia skanowania
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Najbardziej agresywne skanowanie
            // .setReportDelay(0) // Domyślnie 0 - raportuj natychmiast
            .build()

        // 8. Rozpoczęcie skanowania
        try {
            Log.i(TAG_SCAN, "Rozpoczynanie skanowania z filtrem UUID: $SERVICE_UUID...")
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            _isScanning.value = true
            _foundDeviceStatus.value = "Status: Skanowanie..." // Początkowy status
        } catch (e: SecurityException) {
            Log.e(TAG_PERMISSIONS, "Błąd uprawnień podczas startScan mimo wcześniejszego sprawdzenia!", e)
            _foundDeviceStatus.value = "Status: Błąd uprawnień kryt."
            _isScanning.value = false
        } catch (e: IllegalStateException) {
            Log.e(TAG_SCAN, "Błąd stanu podczas startScan (np. Bluetooth wyłączony w międzyczasie?)", e)
            _foundDeviceStatus.value = "Status: Błąd stanu BLE"
            _isScanning.value = false
        } catch (e: Exception) {
            Log.e(TAG_SCAN, "Nieoczekiwany błąd podczas startScan", e)
            _foundDeviceStatus.value = "Status: Błąd startu skan."
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        Log.d(TAG_SCAN, "Wywołano stopScan()")
        if (!_isScanning.value) {
            Log.d(TAG_SCAN, "Skanowanie nie jest aktywne, ignorowanie.")
            return
        }
        // Sprawdzenie uprawnień nie jest konieczne do stopScan, ale nie zaszkodzi
        if (!hasPermissions(getRequiredPermissionsScan())) {
            Log.w(TAG_PERMISSIONS, "Brak uprawnień przy próbie zatrzymania skanowania (powinno mimo to zadziałać).")
        }

        if (bluetoothLeScanner == null) {
            Log.e(TAG_SCAN,"Krytyczny błąd: Skaner jest null, mimo że isScanning=true przy próbie zatrzymania.")
            _isScanning.value = false // Napraw stan
            return
        }

        try {
            Log.i(TAG_SCAN, "Zatrzymywanie skanowania...")
            bluetoothLeScanner?.stopScan(scanCallback)
            // Stan i status aktualizujemy od razu, nie czekamy na callback (stopScan nie ma callbacku sukcesu)
            _isScanning.value = false
            if (!_isAdvertising.value) { // Jeśli rozgłaszanie też wyłączone
                _foundDeviceStatus.value = "Status: Zatrzymano"
            } else { // Jeśli rozgłaszanie nadal aktywne
                _foundDeviceStatus.value = "Status: Rozgłaszanie aktywne"
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG_SCAN, "Błąd stanu podczas stopScan (np. Bluetooth wyłączony)", e)
            _isScanning.value = false // Popraw stan
        } catch (e: Exception) { // Złap inne wyjątki
            Log.e(TAG_SCAN, "Błąd podczas stopScan", e)
            _isScanning.value = false // Popraw stan
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        Log.d(TAG_ADVERTISE, "Wywołano startAdvertising() z ID: $ownUserId")

        // 1. Sprawdzenie uprawnień
        if (!hasPermissions(getRequiredPermissionsAdvertise())) {
            Log.w(TAG_PERMISSIONS, "Brak uprawnień do rozgłaszania.")
            _foundDeviceStatus.value = "Status: Brak uprawnień rozgł."
            return
        }

        // 2. Sprawdzenie, czy Bluetooth jest włączony
        if (bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG_ADVERTISE, "Bluetooth jest wyłączony.")
            _foundDeviceStatus.value = "Status: Włącz Bluetooth"
            return
        }

        // 3. Sprawdzenie wsparcia dla rozgłaszania
        if (bluetoothAdapter?.isMultipleAdvertisementSupported == false) {
            Log.e(TAG_ADVERTISE, "Urządzenie nie wspiera rozgłaszania BLE (isMultipleAdvertisementSupported = false).")
            _foundDeviceStatus.value = "Status: Rozgłaszanie niewspierane"
            return
        }

        // 4. Sprawdzenie, czy już nie rozgłasza
        if (_isAdvertising.value) {
            Log.d(TAG_ADVERTISE, "Rozgłaszanie już aktywne.")
            return
        }

        // 5. Inicjalizacja rozgłaszacza
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG_ADVERTISE, "Nie można uzyskać dostępu do rozgłaszacza BLE (czy urządzenie wspiera rozgłaszanie?).")
            _foundDeviceStatus.value = "Status: Błąd inicj. rozgłaszacza"
            return
        }

        // 6. Ustawienia rozgłaszania
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // Najbardziej agresywne
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)   // Średnia moc
            .setConnectable(false) // Nie oczekujemy połączeń
            .build()

        // 7. Przygotowanie danych
        val parcelUuid = ParcelUuid(SERVICE_UUID)
        val serviceData : ByteArray = try {
            ownUserId.toByteArray(Charset.forName("UTF-8"))
        } catch (e: Exception) {
            Log.e(TAG_ADVERTISE, "Krytyczny błąd: Nie można zakodować UserID '$ownUserId' do UTF-8", e)
            _foundDeviceStatus.value = "Status: Błąd kodowania ID"
            return
        }

        Log.d(TAG_ADVERTISE, "Rozmiar danych usługi (ID) do wysłania: ${serviceData.size} bajtów")
        // Ostrożny limit, można dostosować; Długie UUID (16B) zajmuje dużo miejsca.
        if (serviceData.size > 20) {
            Log.w(TAG_ADVERTISE, "Dane rozgłaszania (ID) mogą być za długie: ${serviceData.size} bajtów. Może spowodować błąd ADVERTISE_FAILED_DATA_TOO_LARGE.")
            // Nie blokujemy, ale ostrzegamy
        }

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Oszczędność miejsca
            .setIncludeTxPowerLevel(false) // Oszczędność miejsca
            .addServiceUuid(parcelUuid) // Kluczowe: dodaj UUID naszej usługi
            .addServiceData(parcelUuid, serviceData) // Kluczowe: dodaj nasze ID jako dane dla tej usługi
            .build()

        // 8. Rozpoczęcie rozgłaszania
        try {
            Log.i(TAG_ADVERTISE, "Rozpoczynanie rozgłaszania z UUID: $SERVICE_UUID i ID: $ownUserId...")
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            // Stan _isAdvertising zostanie ustawiony na true w callbacku onStartSuccess
            // Można ustawić status tymczasowy:
            // _foundDeviceStatus.value = "Status: Uruchamianie rozgł..."
        } catch (e: SecurityException) {
            Log.e(TAG_PERMISSIONS, "Błąd uprawnień podczas startAdvertising mimo wcześniejszego sprawdzenia!", e)
            _foundDeviceStatus.value = "Status: Błąd uprawnień kryt."
            _isAdvertising.value = false
        } catch (e: IllegalStateException) {
            Log.e(TAG_ADVERTISE, "Błąd stanu podczas startAdvertising (np. Bluetooth wyłączony)", e)
            _foundDeviceStatus.value = "Status: Błąd stanu BLE"
            _isAdvertising.value = false
        } catch (e: Exception) {
            Log.e(TAG_ADVERTISE, "Nieoczekiwany błąd podczas startAdvertising", e)
            _foundDeviceStatus.value = "Status: Błąd startu rozgł."
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        Log.d(TAG_ADVERTISE, "Wywołano stopAdvertising()")
        if (!_isAdvertising.value) {
            Log.d(TAG_ADVERTISE, "Rozgłaszanie nie jest aktywne, ignorowanie.")
            return
        }
        // Sprawdzenie uprawnień nie jest konieczne do stopAdvertising
        if (!hasPermissions(getRequiredPermissionsAdvertise())) {
            Log.w(TAG_PERMISSIONS, "Brak uprawnień przy próbie zatrzymania rozgłaszania (powinno mimo to zadziałać).")
        }

        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG_ADVERTISE,"Krytyczny błąd: Advertiser jest null, mimo że isAdvertising=true przy próbie zatrzymania.")
            _isAdvertising.value = false // Popraw stan
            return
        }

        try {
            Log.i(TAG_ADVERTISE, "Zatrzymywanie rozgłaszania...")
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            // Stan i status aktualizujemy od razu
            _isAdvertising.value = false
            if (!_isScanning.value) { // Jeśli skanowanie też wyłączone
                _foundDeviceStatus.value = "Status: Zatrzymano"
            } else { // Jeśli skanowanie nadal aktywne
                _foundDeviceStatus.value = "Status: Skanowanie aktywne"
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG_ADVERTISE, "Błąd stanu podczas stopAdvertising (np. Bluetooth wyłączony)", e)
            _isAdvertising.value = false // Popraw stan
        } catch (e: Exception) { // Złap inne wyjątki
            Log.e(TAG_ADVERTISE, "Błąd podczas stopAdvertising", e)
            _isAdvertising.value = false // Popraw stan
        }
    }

    // --- Funkcje pomocnicze ---

    // Sprawdza, czy lokalizacja systemowa jest włączona
    private fun isLocationEnabled(): Boolean {
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    // --- Funkcje pomocnicze do uprawnień (DEFINIOWANE TYLKO RAZ) ---

    // Zwraca listę *wszystkich* potencjalnie wymaganych uprawnień
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            // ACCESS_FINE_LOCATION jest nadal zalecane dla pełnej funkcjonalności skanowania
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else { // Poniżej Androida 12
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            // ACCESS_FINE_LOCATION jest kluczowe do skanowania BLE poniżej A12
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.toTypedArray()
    }

    // Uprawnienia specyficznie dla skanowania
    fun getRequiredPermissionsScan(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // BLUETOOTH_ADMIN jest potrzebny do start/stopScan poniżej A12
            arrayOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Uprawnienia specyficznie dla rozgłaszania
    fun getRequiredPermissionsAdvertise(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            // BLUETOOTH_ADMIN jest potrzebny do start/stopAdvertising poniżej A12
            arrayOf(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    // Sprawdza, czy wszystkie podane uprawnienia są przyznane
    fun hasPermissions(permissions: Array<String>): Boolean {
        if (permissions.isEmpty()) {
            Log.d(TAG_PERMISSIONS, "hasPermissions: Lista uprawnień do sprawdzenia jest pusta.")
            return true
        }
        val hasAll = permissions.all { permission ->
            val granted = ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG_PERMISSIONS, "Brakujące uprawnienie: $permission")
            }
            granted
        }
        Log.d(TAG_PERMISSIONS, "Sprawdzenie uprawnień [${permissions.joinToString()}]: $hasAll")
        return hasAll
    }

    // --- Companion Object dla funkcji statycznych ---
    companion object {
        // Żąda brakujących uprawnień
        fun requestBluetoothPermissions(activity: ComponentActivity, launcher: ActivityResultLauncher<Array<String>>) {
            // Użyj tymczasowego obiektu tylko do pobrania listy WSZYSTKICH potencjalnie potrzebnych uprawnień
            val allRequiredPermissions = BluetoothManagerUtils(activity, "").getRequiredPermissions()
            val missingPermissions = allRequiredPermissions.filter {
                ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (missingPermissions.isNotEmpty()) {
                Log.i(TAG_PERMISSIONS, "Żądanie brakujących uprawnień: ${missingPermissions.joinToString()}")
                launcher.launch(missingPermissions)
            } else {
                Log.d(TAG_PERMISSIONS, "Wszystkie wymagane uprawnienia (${allRequiredPermissions.joinToString()}) są już przyznane.")
            }
        }

        // Pokazuje wiadomość o odmowie (można rozbudować)
        fun showPermissionDeniedMessage(context: Context, permission: String) {
            Log.w(TAG_PERMISSIONS, "Użytkownik odmówił uprawnienia: $permission. Funkcjonalność może być ograniczona.")
            Toast.makeText(context, "Odmówiono uprawnienia: $permission", Toast.LENGTH_SHORT).show()
        }

        // Otwiera ustawienia lokalizacji systemowej
        fun openLocationSettings(context: Context) {
            Log.d(TAG_LOCATION, "Otwieranie ustawień lokalizacji systemowej...")
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            // Sprawdź, czy jest aktywność obsługująca ten intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Log.e(TAG_LOCATION, "Nie można znaleźć aktywności obsługującej ACTION_LOCATION_SOURCE_SETTINGS.")
                Toast.makeText(context, "Nie można otworzyć ustawień lokalizacji.", Toast.LENGTH_SHORT).show()
            }
        }
    } // Koniec companion object

} // Koniec klasy BluetoothManagerUtils