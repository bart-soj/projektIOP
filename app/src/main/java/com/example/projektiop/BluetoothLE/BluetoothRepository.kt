package com.example.projektiop.BluetoothLE

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.OtherUserRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.util.UUID

private val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // temporary

// Tagi do filtrowania logów w Logcat
private const val TAG_SCAN = "BLE_SCAN_DEBUG"
private const val TAG_ADVERTISE = "BLE_ADVERTISE_DEBUG"
private const val TAG_LOCATION = "BLE_LOCATION_CHECK"
private const val TAG_PERMISSIONS = "BLE_PERMISSIONS"

private const val ID: String = "_id"

class BluetoothRepository(private val context: Context) {
    private val ownUserId = SharedPreferencesRepository.get(ID, "")
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private val permissionsManager by lazy {
        BTPermissionsManager(context)
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val _foundDeviceStatus = MutableStateFlow("Status: Oczekuje...")
    val foundDeviceStatus = _foundDeviceStatus.asStateFlow()

    private val _foundDeviceIds = MutableStateFlow<List<String>>(emptyList())
    val foundDeviceIds: StateFlow<List<String>> = _foundDeviceIds.asStateFlow()

    // --- Skanowanie ---
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                val deviceAddress = scanResult.device?.address ?: "Brak adresu"
                val record = scanResult.scanRecord ?: return@let
                val serviceUuids = record.serviceUuids
                val targetParcelUuid = ParcelUuid(SERVICE_UUID)

                if (serviceUuids?.contains(targetParcelUuid) == true) {
                    val serviceData = record.getServiceData(targetParcelUuid)
                    if (serviceData != null) {
                        try {
                            val foundUserId = String(serviceData, Charset.forName("UTF-8")).trim()

                            // Sprawdzamy, czy ID jest niepuste i czy jest nowe
                            if (foundUserId.isNotEmpty() && !_foundDeviceIds.value.contains(foundUserId)) {
                                Log.i(TAG_SCAN, ">>> NOWE URZĄDZENIE: Znaleziono ID: $foundUserId (MAC: $deviceAddress) <<<")
                                _foundDeviceStatus.value = "Status: Znaleziono $foundUserId"
                                val updatedList = _foundDeviceIds.value + foundUserId
                                _foundDeviceIds.value = updatedList
                                _foundDeviceStatus.value = "Status: Znaleziono (${foundDeviceIds.value.size}): ${foundDeviceIds.value.joinToString()}"
                            } else if (foundUserId.isNotEmpty()) {
                                // ID już znane w tej sesji skanowania
                                Log.v(TAG_SCAN, "onScanResult: Ponownie wykryto urządzenie z ID: $foundUserId (MAC: $deviceAddress)")
                            } else {
                                // Otrzymano puste dane
                                Log.w(TAG_SCAN, "onScanResult: Otrzymano puste ID od urządzenia $deviceAddress")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG_SCAN, "Błąd dekodowania danych usługi dla $deviceAddress", e)
                            // decoding failed
                        }
                    } else {
                        // UUID pasuje, ale brak danych (ServiceData)
                    }
                } else {
                    // UUID nie pasuje (inne urządzenie BLE)
                }
            } ?: run {
                // empty result
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG_SCAN, "Skanowanie nie powiodło się, kod błędu: $errorCode")
            _isScanning.value = false
            val errorText = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan Failed: Already Started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Scan Failed: App Registration Failed (Check Manifest?)"
                SCAN_FAILED_INTERNAL_ERROR -> "Scan Failed: Internal Error"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan Failed: Feature Unsupported (BLE Scan not supported?)"
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
    private var advertisingSet: AdvertisingSet? = null

    private val advertiseSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet?,
            txPower: Int,
            status: Int
        ) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status)
            if (status == ADVERTISE_SUCCESS) {
                Log.i(TAG_ADVERTISE, ">>> Rozgłaszanie rozpoczęte pomyślnie (ID: $ownUserId, UUID: $SERVICE_UUID) <<<")
                this@BluetoothRepository.advertisingSet = advertisingSet
                _isAdvertising.value = true
            } else {
                Log.e(TAG_ADVERTISE, "Rozgłaszanie nie powiodło się, kod błędu: $status")
                _isAdvertising.value = false
                _foundDeviceStatus.value = "Status: Błąd rozgł. ($status)"
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            super.onAdvertisingSetStopped(advertisingSet)
            Log.i(TAG_ADVERTISE, "Rozgłaszanie zatrzymane")
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!permissionsManager.hasPermissions(permissionsManager.getRequiredPermissionsScan())) {
            _foundDeviceStatus.value = "Status: Brak uprawnień skan."
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            _foundDeviceStatus.value = "Status: Włącz Bluetooth"
            return
        }

        if (!permissionsManager.isLocationEnabled()) {
            _foundDeviceStatus.value = "Status: Włącz Lokalizację"
        }

        if (_isScanning.value) {
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.let { adapter ->
            adapter.bluetoothLeScanner
        }
        if (bluetoothLeScanner == null) {
            _foundDeviceStatus.value = "Status: Błąd inicj. skanera"
            return
        }

        _foundDeviceIds.value = emptyList<String>()

        // Ustawienia skanowania
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val scanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            // .setReportDelay(0) // Domyślnie 0 - raportuj natychmiast
            .build()

        try {
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            _isScanning.value = true
            _foundDeviceStatus.value = "Status: Skanowanie..."
        } catch (e: SecurityException) {
            _foundDeviceStatus.value = "Status: Błąd uprawnień kryt."
            _isScanning.value = false
        } catch (e: IllegalStateException) {
            _foundDeviceStatus.value = "Status: Błąd stanu BLE"
            _isScanning.value = false
        } catch (e: Exception) {
            _foundDeviceStatus.value = "Status: Błąd startu skan."
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) {
            return
        }

        /*
        if (!hasPermissions(getRequiredPermissionsScan())) {
        } */

        if (bluetoothLeScanner == null) {
            _isScanning.value = false
            return
        }

        try {
            bluetoothLeScanner!!.stopScan(scanCallback)
            _isScanning.value = false
            if (!_isAdvertising.value) {
                _foundDeviceStatus.value = "Status: Zatrzymano"
            } else {
                _foundDeviceStatus.value = "Status: Rozgłaszanie aktywne"
            }
        } catch (e: IllegalStateException) {
            _isScanning.value = false
        } catch (e: Exception) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        if (!permissionsManager.hasPermissions(permissionsManager.getRequiredPermissionsAdvertise())) {
            _foundDeviceStatus.value = "Status: Brak uprawnień rozgł."
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            _foundDeviceStatus.value = "Status: Włącz Bluetooth"
            return
        }

        if (bluetoothAdapter?.isLeExtendedAdvertisingSupported == false) {
            _foundDeviceStatus.value = "Status: Rozgłaszanie niewspierane"
            return
        }

        if (_isAdvertising.value) {
            return
        }

        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            _foundDeviceStatus.value = "Status: Błąd inicj. rozgłaszacza"
            return
        }

        // Ustawienia rozgłaszania
        val settings = AdvertisingSetParameters.Builder()
            .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .setLegacyMode(false)
            .setConnectable(false)
            .build()

        val parcelUuid = ParcelUuid(SERVICE_UUID)
        val serviceData : ByteArray = try {
            ownUserId.toByteArray(Charset.forName("UTF-8"))
        } catch (e: Exception) {
            _foundDeviceStatus.value = "Status: Błąd kodowania ID"
            return
        }

        /* not important for BLE
        if (serviceData.size > 20) {
        } */

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(parcelUuid)
            .addServiceData(parcelUuid, serviceData)
            .build()

        // 8. Rozpoczęcie rozgłaszania
        try {
            bluetoothLeAdvertiser?.startAdvertisingSet(settings, data, null, null, null, advertiseSetCallback)
            // Stan _isAdvertising zostanie ustawiony na true w callbacku onStartSuccess
            // Można ustawić status tymczasowy:
            // _foundDeviceStatus.value = "Status: Uruchamianie rozgł..."
        } catch (e: SecurityException) {
            _foundDeviceStatus.value = "Status: Błąd uprawnień kryt."
            _isAdvertising.value = false
        } catch (e: IllegalStateException) {
            _foundDeviceStatus.value = "Status: Błąd stanu BLE"
            _isAdvertising.value = false
        } catch (e: Exception) {
            _foundDeviceStatus.value = "Status: Błąd startu rozgł."
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (!_isAdvertising.value) {
            return
        }
        /*
        if (!hasPermissions(getRequiredPermissionsAdvertise())) {
        } */

        if (bluetoothLeAdvertiser == null) {
            _isAdvertising.value = false
            return
        }

        try {
            bluetoothLeAdvertiser?.stopAdvertisingSet(advertiseSetCallback)
            _isAdvertising.value = false
            if (!_isScanning.value) {
                _foundDeviceStatus.value = "Status: Zatrzymano"
            } else {
                _foundDeviceStatus.value = "Status: Skanowanie aktywne"
            }
        } catch (e: IllegalStateException) {
            _isAdvertising.value = false
        } catch (e: Exception) {
            _isAdvertising.value = false
        }
    }
}