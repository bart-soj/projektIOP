package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projektiop.BluetoothLE.BLEViewModel
import com.example.projektiop.R
import com.example.projektiop.activeHandshake.NFC.ActiveHandshakeButton
import com.example.projektiop.data.api.CertificateRequest
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.util.CertificateUtils
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.launch


private const val ID: String = "_id"
private const val BASE_URL_KEY: String = "BASE_URL"


@OptIn(ExperimentalMaterial3Api::class) // Dla Scaffold
@Composable
fun ScannerScreen(modifier: Modifier = Modifier, navController: NavController, viewModel: BLEViewModel) {
    val context = LocalContext.current

    // Pobranie aktualnej ścieżki
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val users by viewModel.userProfiles.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp), // Dodatkowy padding wewnętrzny
            horizontalAlignment = Alignment.CenterHorizontally // Wycentruj elementy
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Odstęp od góry

            // Tytuł Ekranu
            Text(
                text = stringResource(R.string.scanner_screen_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            /*
            Text(
                text = stringResource(R.string.scanner_your_identifier_label, ""), // Dodaj zasób string "Twój identyfikator: %s"
                style = MaterialTheme.typography.bodyLarge
            )
            */

            // Status BLE
            ScanStatus(viewModel)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                if (!isScanning) {
                    viewModel.startScan()
                } else {
                    viewModel.stopScan()
                }
            }
            ) {
                if (!isScanning) {
                    Text(stringResource(R.string.scanner_start_scanning))
                } else {
                    Text(stringResource(R.string.scanner_stop_scanning))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                if (!isAdvertising) {
                    viewModel.startAdvertising()
                } else {
                    viewModel.stopAdvertising()
                }
            }
            ) {
                if (!isAdvertising) {
                    Text(stringResource(R.string.scanner_start_advertising))
                } else {
                    Text(stringResource(R.string.scanner_stop_advertising))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Column {
                ScannedUsersList(users = users) { user ->
                    navController.navigate(
                        "friend_profile/${user._id}?username=${user.username}&displayName=${user.profile?.displayName}&avatarUrl=${user.profile?.avatarUrl}"
                    )
                }
                ActiveHandshakeButton { }
                //CertificateRequester(AuthRepository.getToken().toString())
            }

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp na dole
        }
    }
}

@Composable
fun ScanStatus(viewModel: BLEViewModel, modifier: Modifier = Modifier) {
    val isScanning = viewModel.isScanning.collectAsState().value
    val isAdvertising = viewModel.isAdvertising.collectAsState().value

    val statusText = when {
        isScanning && isAdvertising -> stringResource(R.string.ble_status_scanning_and_advertising)
        isScanning -> stringResource(R.string.ble_status_scanning)
        isAdvertising -> stringResource(R.string.ble_status_advertising)
        else -> stringResource(R.string.ble_status_inactive)
    }

    Text(
        text = stringResource(R.string.ble_status_label, statusText),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}


@Composable
fun CertificateRequester(
    authToken: String,
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
                            Result.failure(e)
                        }

                       profileResult.fold(
                            onSuccess = { profileData ->
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
                                println("Error fetching user profile: ${exception.message}")
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
    user: UserProfileResponse,
    onClick: (UserProfileResponse) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

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
            val rawUrl = user.profile?.avatarUrl
            val fullUrl = rawUrl?.let { if (it.startsWith("http")) it else "${SharedPreferencesRepository.get(BASE_URL_KEY, "")}$it" }
            if (fullUrl != null) {
                val req = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(fullUrl)
                    .crossfade(true)
                    .apply {
                        val token = com.example.projektiop.data.repositories.AuthRepository.getToken()
                        if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
                    }
                    .build()
                AsyncImage(
                    model = req,
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.avatar_placeholder),
                    error = painterResource(R.drawable.avatar_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.profile?.displayName ?: user.username.toString(), style = MaterialTheme.typography.titleMedium)
                Text(user.username.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(user.email.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                /*
                if (!user.interests.isNullOrEmpty()) {
                    Text(
                        text = "Zainteresowania: ${user.interests.joinToString { it.interest.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                */
            }

            Button(onClick = {
                coroutineScope.launch{ FriendshipRepository.sendFriendRequest(user._id.toString()) }
            }
            ) {
                Text("Dodaj") // TODO() better repository add function that takes already existing friend into account
            }

        }
    }
}


@Composable
fun ScannedUsersList(
    users: List<UserProfileResponse>?,
    onUserClick: (UserProfileResponse) -> Unit
) {

    if (users == null) {
        CircularProgressIndicator()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(users) { user ->
                ScannedUserRow(user, onClick = { onUserClick(user) })
            }
        }
    }
}


fun cosineSimilarity(a: Set<String>, b: Set<String>): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0

    val intersectionSize = a.intersect(b).size
    return intersectionSize / kotlin.math.sqrt(a.size.toDouble() * b.size.toDouble())
}


// --- Podgląd dla ScannerScreen (opcjonalnie) ---
@Preview(showBackground = true)
@Composable
fun ScannerScreenPreview() {
    MaterialTheme { // Użyj swojego motywu
        // Przekaż przykładowe dane i pusty NavController dla podglądu
        ScannerScreen(navController = rememberNavController(), viewModel = BLEViewModel(
            application = TODO()
        ))
    }
}
