package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import com.example.projektiop.BluetoothLE.BluetoothManagerUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.* // Material 3
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R // Upewnij się, że masz odpowiednie zasoby string
import com.example.projektiop.data.api.CertificateRequest
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.CertificateUtils
import com.example.projektiop.data.api.Profile
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.repositories.FriendItem
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.launch


private const val ID: String = "_id"


@OptIn(ExperimentalMaterial3Api::class) // Dla Scaffold
@Composable
fun ScannerScreen(modifier: Modifier = Modifier, navController: NavController) {
    val name: String = SharedPreferencesRepository.get(ID, "brak")
    val context = LocalContext.current
    // Pamiętaj o bleManager - kluczowa logika pozostaje bez zmian
    val bleManager = remember { BluetoothManagerUtils(context, name) }
    val devices by bleManager.foundDeviceIds.collectAsState()

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
                 // .verticalScroll(rememberScrollState()) // Umożliw przewijanie
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

            // Spacer(modifier = Modifier.height(16.dp))
            // CertificateRequester(AuthRepository.getToken().toString())
            ScannedUsersList(deviceIds = devices) {}

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


@Composable
fun CertificateRequester(
    authToken: String,   // Bearer token from your login
) {
    var result by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val api = RetrofitInstance.certificateApi

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Request Certificate", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    result = null
                    try {
                        val profileResult = try {
                            UserRepository.fetchMyProfile()
                        } catch (e: Exception) {
                            kotlin.Result.failure(e)
                        }

                       profileResult.fold(
                            onSuccess = { profileData ->
                                // Successfully fetched user profile
                                val userEmail: String = profileData.email.toString()
                                println("User email: $userEmail")
                                try {
                                    val keyPair = CertificateUtils.generateKeyPair()
                                    var csrPem = CertificateUtils.generateCSR(userEmail, keyPair)
                                    println("CSR generated: $csrPem")
                                    val certificateResponse = try {
                                        api.issueCertificate(
                                            token = "Bearer $authToken",
                                            request = CertificateRequest(csrPem)
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }

                                    result = if (certificateResponse != null) {
                                        certificateResponse.certPem
                                    } else {
                                        "Error: Failed to issue certificate"
                                    }
                                } catch (e: Exception) {
                                    println(e)
                                }
                            },
                            onFailure = { exception ->
                                // Handle the error state
                                println("Error fetching user profile: ${exception.message}")
                                // Cannot generate CSR because profile fetching failed
                            })


                    } catch (e: Exception) {
                        error = "Failed: ${e.localizedMessage}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading
        ) {
            Text(if (loading) "Requesting..." else "Request Certificate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            result != null -> Text(result!!, style = MaterialTheme.typography.bodySmall)
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}


@Composable
fun ScannedUserRow(
    userId: String,
    onClick: (UserProfileResponse) -> Unit
) {
    // Use produceState to fetch user profile asynchronously
    val userState = produceState<UserProfileResponse?>(initialValue = null, userId) {
        value = try {
            val result = UserRepository.fetchUserById(userId) // suspend fun returning Result<UserProfileResponse>
            result.getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    userState.value?.let { user ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onClick(user) },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Placeholder avatar
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(user.profile?.displayName ?: user.username, style = MaterialTheme.typography.titleMedium)
                    Text(user.username, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (!user.interests.isNullOrEmpty()) {
                        Text(
                            text = "Zainteresowania: ${user.interests.joinToString { it.interest.name }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Optional action button (chat, etc.)
                Button(onClick = { /* handle chat/navigation */ }) {
                    Text("Czat")
                }
            }
        }
    } ?: run {
        // Loading placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}


@Composable
fun ScannedUsersList(
    deviceIds: List<String>,
    onUserClick: (UserProfileResponse) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(deviceIds, key = { it }) { deviceId ->
            ScannedUserRow(userId = deviceId, onClick = onUserClick)
        }
    }
}


// --- Podgląd dla ScannerScreen (opcjonalnie) ---
@Preview(showBackground = true)
@Composable
fun ScannerScreenPreview() {
    MaterialTheme { // Użyj swojego motywu
        // Przekaż przykładowe dane i pusty NavController dla podglądu
        ScannerScreen(navController = rememberNavController())
    }
}