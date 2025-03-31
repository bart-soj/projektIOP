package com.example.projektiop // Upewnij się, że pakiet jest poprawny

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme // Dodano import dla stylu tekstu
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign // Dodano import dla wyrównania tekstu
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Ważny import
import com.example.projektiop.ui.theme.ProjektIOPTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    // Launcher do obsługi wyników żądania uprawnień
    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.forEach { (permission, granted) ->
                if (!granted) {
                    allGranted = false
                    // Używamy funkcji z companion object naszej klasy Utils
                    BluetoothManagerUtils.showPermissionDeniedMessage(this, permission)
                }
            }
            if (allGranted) {
                Log.i("PERMISSIONS_RESULT", "Wszystkie wymagane uprawnienia przyznane po żądaniu.")
                // Można tutaj spróbować ponownie zainicjować jakąś funkcjonalność,
                // np. jeśli użytkownik nadał uprawnienia po wcześniejszej odmowie.
            } else {
                Log.w("PERMISSIONS_RESULT", "Nie wszystkie wymagane uprawnienia zostały przyznane.")
                // Można pokazać użytkownikowi trwałe powiadomienie,
                // że aplikacja może nie działać poprawnie.
            }
        }

    // Generujemy ID użytkownika raz przy tworzeniu Activity
    private val userId: String by lazy { generateUserId() } // Użycie lazy dla pewności inicjalizacji

    // Tworzymy instancję managera BLE - lateinit, bo potrzebuje contextu, inicjowana w onCreate
    private lateinit var bleManager: BluetoothManagerUtils


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Włączenie rysowania pod paskami systemowymi

        // Inicjalizujemy managera BLE tutaj, przekazując context (this) i userId
        bleManager = BluetoothManagerUtils(this, userId)

        setContent {
            ProjektIOPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Przekazujemy userId i zainicjalizowany bleManager do głównego ekranu
                    MainScreen(
                        userId = userId,
                        bleManager = bleManager,
                        modifier = Modifier
                            .padding(innerPadding) // Stosujemy padding od Scaffold
                            .fillMaxSize() // Upewniamy się, że Column wypełnia dostępną przestrzeń
                    )
                }
            }
        }

        // Żądamy uprawnień po ustawieniu contentu i zainicjalizowaniu managera
        BluetoothManagerUtils.requestBluetoothPermissions(this, permissionsLauncher)
    }

    // Prosta funkcja do generowania losowego ID (8 znaków)
    private fun generateUserId(): String {
        val newId = UUID.randomUUID().toString().substring(0, 8).uppercase()
        Log.d("USER_ID", "Wygenerowano ID użytkownika: $newId")
        return newId
    }

    // Zatrzymujemy skanowanie/rozgłaszanie przy zamykaniu aplikacji (onDestroy)
    override fun onDestroy() {
        super.onDestroy()
        // Sprawdź, czy bleManager został zainicjalizowany przed użyciem
        if (::bleManager.isInitialized) {
            Log.i("MainActivity", "Zatrzymywanie operacji BLE w onDestroy...")
            bleManager.stopScan()
            bleManager.stopAdvertising()
        } else {
            Log.w("MainActivity", "onDestroy: bleManager nie został zainicjalizowany.")
        }
    }

    // Można też rozważyć zatrzymywanie w onStop() i wznawianie w onStart(),
    // jeśli aplikacja ma działać tylko na pierwszym planie.
    // override fun onStop() {
    //     super.onStop()
    //     if (::bleManager.isInitialized) {
    //         bleManager.stopScan()
    //         bleManager.stopAdvertising()
    //     }
    // }
}

// Główny ekran aplikacji (Composable)
@Composable
fun MainScreen(
    userId: String,                     // ID tego użytkownika
    bleManager: BluetoothManagerUtils,  // Instancja managera BLE
    modifier: Modifier = Modifier       // Modyfikator przekazany z zewnątrz (zawiera padding od Scaffold)
) {
    val context = LocalContext.current // Pobieramy context dla przycisku ustawień lokalizacji

    // Zbieramy stany z BluetoothManagerUtils w sposób świadomy cyklu życia
    // Te stany (isScanning, isAdvertising) kontrolują dostępność przycisków
    val isScanning by bleManager.isScanning.collectAsStateWithLifecycle()
    val isAdvertising by bleManager.isAdvertising.collectAsStateWithLifecycle()
    // Ten stan (deviceStatus) wyświetla aktualny status operacji BLE
    val deviceStatus by bleManager.foundDeviceStatus.collectAsStateWithLifecycle()

    Column(
        modifier = modifier // Używamy modyfikatora przekazanego z góry (z paddingiem)
            .padding(16.dp), // Dodatkowy wewnętrzny padding dla elementów w kolumnie
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Rozmieszczamy od góry
    ) {
        // Wyświetlanie ID użytkownika
        Text(
            text = "Twoje ID: $userId",
            style = MaterialTheme.typography.titleMedium // Użycie stylu z motywu
        )

        Spacer(modifier = Modifier.height(24.dp)) // Większy odstęp

        // Przyciski kontrolne Skanowania
        Row(
            modifier = Modifier.fillMaxWidth(), // Rozciągnij wiersz
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally) // Odstęp i wyśrodkowanie
        ) {
            Button(
                onClick = { bleManager.startScan() },
                enabled = !isScanning // Wyłącz przycisk, jeśli już skanuje
            ) {
                Text("Start Skan")
            }
            Button(
                onClick = { bleManager.stopScan() },
                enabled = isScanning // Wyłącz przycisk, jeśli nie skanuje
            ) {
                Text("Stop Skan")
            }
        }

        Spacer(modifier = Modifier.height(12.dp)) // Mniejszy odstęp

        // Przyciski kontrolne Rozgłaszania
        Row(
            modifier = Modifier.fillMaxWidth(), // Rozciągnij wiersz
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally) // Odstęp i wyśrodkowanie
        ) {
            Button(
                onClick = { bleManager.startAdvertising() },
                enabled = !isAdvertising // Wyłącz przycisk, jeśli już rozgłasza
            ) {
                Text("Start Rozgł.")
            }
            Button(
                onClick = { bleManager.stopAdvertising() },
                enabled = isAdvertising // Wyłącz przycisk, jeśli nie rozgłasza
            ) {
                Text("Stop Rozgł.")
            }
        }

        Spacer(modifier = Modifier.height(32.dp)) // Jeszcze większy odstęp przed statusem

        // Wyświetlanie GŁÓWNEGO statusu z BluetoothManagerUtils
        Text(
            text = deviceStatus, // Ten status jest aktualizowany przez bleManager
            style = MaterialTheme.typography.bodyLarge, // Użycie stylu z motywu
            textAlign = TextAlign.Center, // Wyśrodkowanie tekstu
            modifier = Modifier.padding(horizontal = 16.dp) // Dodatkowy padding dla tekstu statusu
        )

        // Przycisk do otwierania ustawień lokalizacji, jeśli status tego wymaga
        if (deviceStatus == "Status: Włącz Lokalizację") {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { BluetoothManagerUtils.openLocationSettings(context) }) {
                Text("Otwórz Ustawienia Lokalizacji")
            }
        }

        // Można dodać więcej elementów UI poniżej, jeśli potrzeba
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun DefaultPreview() {
    // Używamy remember do stworzenia managera tylko raz dla podglądu
    val previewContext = LocalContext.current
    val previewBleManager = remember { BluetoothManagerUtils(previewContext, "PREVIEW_ID") }

    // --- USUNIĘTO PRÓBĘ PRZYPISANIA WARTOŚCI ---
    // Nie próbujemy już modyfikować stanu managera w podglądzie w ten sposób:
    // LaunchedEffect(Unit) {
    //     // !!! TE LINIE POWODOWAŁY BŁĄD "Val cannot be reassigned" i zostały usunięte !!!
    //     // previewBleManager.foundDeviceStatus.value = "Status: Oczekuje (Podgląd)"
    //     // previewBleManager.isScanning.value = true // To też by nie zadziałało
    // }
    // Podgląd po prostu użyje stanu początkowego z managera.

    ProjektIOPTheme {
        // Używamy Surface, aby podgląd miał tło z motywu
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            // Wywołujemy MainScreen z managerem podglądu.
            // MainScreen wewnątrz użyje collectAsStateWithLifecycle,
            // aby pobrać stan początkowy z previewBleManager.
            MainScreen(userId = "USER_PREV", bleManager = previewBleManager)
        }
    }
}