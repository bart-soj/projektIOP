package com.example.projektiop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.shape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.projektiop.ui.viewmodels.ScannerViewModel
import com.example.projektiop.ui.viewmodels.UserWithStatus
import com.example.projektiop.R
import com.example.projektiop.activeHandshake.NFC.ActiveHandshakeButton
import com.example.projektiop.data.api.CertificateApi
import com.example.projektiop.data.api.CertificateRequest
import com.example.projektiop.util.CertificateUtils
import com.example.projektiop.data.db.realm.objects.User
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.models.FriendshipStatus
import com.example.projektiop.ui.components.UserAvatar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


private const val ID: String = "_id"
private const val BASE_URL_KEY: String = "BASE_URL"


@OptIn(ExperimentalMaterial3Api::class) // Dla Scaffold
@Composable
fun ScannerScreen( navController: NavController) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val viewModel = koinViewModel<ScannerViewModel>()

    val users by viewModel.users.collectAsState()
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

            Button(
                onClick = {
                    if (!isScanning) {
                        viewModel.startScan()
                    } else {
                        viewModel.stopScan()
                    }
                },
                colors = buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (!isScanning) {
                    Text(stringResource(R.string.scanner_start_scanning))
                } else {
                    Text(stringResource(R.string.scanner_stop_scanning))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isAdvertising) {
                        viewModel.startAdvertising()
                    } else {
                        viewModel.stopAdvertising()
                    }
                },
                colors = buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (!isAdvertising) {
                    Text(stringResource(R.string.scanner_start_advertising))
                } else {
                    Text(stringResource(R.string.scanner_stop_advertising))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Column {
                ScannedUsersList(
                    users = users,
                    onUserClick = { user ->
                        navController.navigate(
                            "friend_profile/${user._id.toHexString()}?username=${user.username}&displayName=${user.profile?.displayName}&avatarUrl=${user.profile?.avatarUrl}"
                        )
                    },
                    onAddClick = { id: String ->
                        viewModel.addFriend(id)
                    },
                    onChatClick = { id: String ->
                        //navController.navigate("chat_detail?chatId=null&friendId=${}")
                        navController.navigate("chats")
                    },
                )

                ActiveHandshakeButton { }
                //CertificateRequester(AuthRepository.getToken().toString())
            }

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp na dole
        }
    }
}

@Composable
fun ScanStatus(viewModel: ScannerViewModel, modifier: Modifier = Modifier) {
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
fun ScannedUserRow(
    userWithStatus: UserWithStatus,
    onClick: (User) -> Unit,
    onAddClick: (String) -> Unit,
    onChatClick: (String) -> Unit
) {
    val user = userWithStatus.user
    val status = userWithStatus.status

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
            UserAvatar(user.profile?.avatarUrl, modifier = Modifier.size(56.dp) )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.profile?.displayName ?: user.username.toString(), style = MaterialTheme.typography.titleMedium)
                Text(user.username.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(user.email.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            when (status) {
                FriendshipStatus.NOT_FRIENDS-> Button(onClick = { onAddClick(user._id.toHexString()) }) { Text(stringResource(R.string.add)) }
                FriendshipStatus.PENDING -> Button(onClick = {}, enabled = false) { Text(stringResource(R.string.sent)) }
                FriendshipStatus.ACCEPTED -> Button(onClick = { onChatClick(user._id.toHexString()) }) {Text(stringResource(R.string.chat))}
                FriendshipStatus.BLOCKED -> Button(onClick = {}, enabled = false) { stringResource(R.string.blocked) }
                FriendshipStatus.REJECTED -> Button(onClick = {}, enabled = false) { Text(stringResource(R.string.rejected)) }
            }
        }
    }
}


@Composable
fun ScannedUsersList(
    users: List<UserWithStatus>?,
    onUserClick: (User) -> Unit,
    onAddClick: (String) -> Unit,
    onChatClick: (String) -> Unit
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
                ScannedUserRow(
                    user,
                    onClick = { onUserClick(user.user) },
                    onAddClick = onAddClick,
                    onChatClick = onChatClick
                )
            }
        }
    }
}





@Composable
fun CertificateRequester(
    authToken: String,
) {
    var result by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val certificateApi = koinInject<CertificateApi>()
    val userRepository = koinInject<UserRepository>()

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
                            userRepository.fetchMyProfile()
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
                                        certificateApi.issueCertificate(
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