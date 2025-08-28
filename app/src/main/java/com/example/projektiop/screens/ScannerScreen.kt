package com.example.projektiop.screens

import com.example.projektiop.BluetoothLE.BluetoothManagerUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.* // Material 3
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R // Upewnij się, że masz odpowiednie zasoby string

@OptIn(ExperimentalMaterial3Api::class) // Dla Scaffold
@Composable
fun ScannerScreen(name: String, modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    // Pamiętaj o bleManager - kluczowa logika pozostaje bez zmian
    val bleManager = remember { BluetoothManagerUtils(context, name) }

    // Pobranie aktualnej ścieżki dla dolnego paska nawigacji
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Użyj tego samego komponentu BottomNavigationBar co w MainScreen
            // Upewnij się, że BottomNavigationBar jest zdefiniowany w dostępnym miejscu
            // (np. w osobnym pliku lub w MainScreen.kt, jeśli ScannerScreen jest w tym samym pakiecie
            // lub BottomNavigationBar jest zadeklarowany jako public)
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues -> // paddingValues zawiera padding od Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Zastosuj padding od Scaffold
                .verticalScroll(rememberScrollState()) // Umożliw przewijanie
                .padding(16.dp), // Dodatkowy padding wewnętrzny (poziomy i pionowy)
            horizontalAlignment = Alignment.CenterHorizontally // Wycentruj elementy w kolumnie
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Odstęp od góry

            // Tytuł Ekranu (opcjonalnie, dla lepszej orientacji)
            Text(
                text = stringResource(R.string.scanner_screen_title), // Dodaj zasób string dla "Skaner BLE"
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Wyświetlanie przekazanego imienia/identyfikatora
            // Usunięto 'modifier = modifier' stąd, bo główny modifier jest na Column
            Text(
                text = stringResource(R.string.scanner_your_identifier_label, name), // Dodaj zasób string "Twój identyfikator: %s"
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status BLE - Wyświetlamy go przed przyciskami dla lepszej widoczności
            ScanStatus(bleManager)

            Spacer(modifier = Modifier.height(24.dp))

            // Przyciski sterujące BLE
            Button(onClick = { bleManager.startScan() }) {
                Text(stringResource(R.string.scanner_start_scanning)) // Dodaj zasób string
            }
            Spacer(modifier = Modifier.height(8.dp)) // Odstęp między przyciskami

            Button(onClick = { bleManager.stopScan() }) {
                Text(stringResource(R.string.scanner_stop_scanning)) // Dodaj zasób string
            }

            Spacer(modifier = Modifier.height(16.dp)) // Większy odstęp między grupami przycisków

            Button(onClick = { bleManager.startAdvertising() }) {
                Text(stringResource(R.string.scanner_start_advertising)) // Dodaj zasób string
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { bleManager.stopAdvertising() }) {
                Text(stringResource(R.string.scanner_stop_advertising)) // Dodaj zasób string
            }

            // Przycisk "Powrót" jest już niepotrzebny, ponieważ nawigacją zarządza
            // systemowy przycisk wstecz oraz BottomNavigationBar.
            // Usunięto: Button(onClick = { navController.popBackStack() }, ...)

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp na dole przed końcem scrolla/bottom bar
        }
    }
}

// Komponent ScanStatus pozostaje bez zmian
@Composable
fun ScanStatus(bleManager: BluetoothManagerUtils, modifier: Modifier = Modifier) {
    val isScanning = bleManager.isScanning.collectAsState().value
    val isAdvertising = bleManager.isAdvertising.collectAsState().value

    // Użyj zasobów string dla lepszej internacjonalizacji
    val statusText = when {
        isScanning && isAdvertising -> stringResource(R.string.ble_status_scanning_and_advertising)
        isScanning -> stringResource(R.string.ble_status_scanning)
        isAdvertising -> stringResource(R.string.ble_status_advertising)
        else -> stringResource(R.string.ble_status_inactive)
    }

    Text(
        text = stringResource(R.string.ble_status_label, statusText), // Np. "Status BLE: %s"
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}


// --- Podgląd dla ScannerScreen (opcjonalnie) ---
@Preview(showBackground = true)
@Composable
fun ScannerScreenPreview() {
    MaterialTheme { // Użyj swojego motywu
        // Przekaż przykładowe dane i pusty NavController dla podglądu
        ScannerScreen(name = "Podgląd ID", navController = rememberNavController())
    }
}