package com.example.projektiop.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.projektiop.R
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.FriendItem
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController, darkMode: Boolean, onToggleDark: () -> Unit) {

    var animationPlayed by remember { mutableStateOf(false) } // Flaga, by animacja zagrała raz
    val alphaAnimation = animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f, // Cel: 1 (widoczne) lub 0 (niewidocne)
        animationSpec = tween(durationMillis = 1000) // Czas trwania animacji (1 sekunda)
    )

    // Uruchom zmianę targetValue, gdy kompozycja się pojawi po raz pierwszy
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController, currentRoute = currentRoute) }
    ) { paddingValues ->
    var showBlockedDialog by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 32.dp)
                    .graphicsLayer { alpha = alphaAnimation.value },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                // Dark mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.dark_mode_label), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = darkMode, onCheckedChange = { onToggleDark() })
                }

                Button(
                    onClick = { showBlockedDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Zablokowani użytkownicy") }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        AuthRepository.clearToken()
                        navController.navigate("start")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Wyloguj się", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (showBlockedDialog) {
            BlockedUsersDialog(onClose = { showBlockedDialog = false })
        }
    }
}

@Composable
private fun BlockedUsersDialog(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<FriendItem>>(emptyList()) }
    var processingId by remember { mutableStateOf<String?>(null) }
    var myUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            // fetch profile for current user id
            com.example.projektiop.data.repositories.UserRepository.fetchMyProfile().onSuccess { myUserId = it._id }
            FriendshipRepository.fetchBlocked()
                .onSuccess { items = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text("Zamknij") } },
        title = { Text("Zablokowani użytkownicy") },
        text = {
            when {
                loading -> { CircularProgressIndicator() }
                error != null -> { Text(error ?: "Błąd", color = MaterialTheme.colorScheme.error) }
                items.isEmpty() -> { Text("Brak zablokowanych użytkowników") }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEach { u ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(u.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(u.username, style = MaterialTheme.typography.bodySmall)
                                    }
                                    val isProcessing = processingId == u.friendshipId
                                    val showUnblock = myUserId != null && myUserId == u.blockedBy
                                    if (showUnblock) {
                                        TextButton(enabled = !isProcessing, onClick = {
                                            scope.launch {
                                                processingId = u.friendshipId
                                                FriendshipRepository.unblockFriendship(u.friendshipId)
                                                    .onSuccess {
                                                        FriendshipRepository.fetchBlocked().onSuccess { items = it }
                                                    }
                                                processingId = null
                                            }
                                        }) { Text(if (isProcessing) "..." else "Odblokuj") }
                                    } else {
                                        Text("Zablokowany", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(navController = NavController(LocalContext.current), darkMode = false, onToggleDark = {})
}