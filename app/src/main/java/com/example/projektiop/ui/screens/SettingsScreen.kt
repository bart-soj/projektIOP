package com.example.projektiop.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.screens.components.FriendCard
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    darkMode: Boolean,
    onToggleDark: () -> Unit
) {

    var animationPlayed by remember { mutableStateOf(false) }
    val alphaAnimation = animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController, currentRoute = currentRoute) }
    ) { paddingValues ->

        var showLogoutDialog by remember { mutableStateOf(false) }
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
                // Tryb ciemny
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.dark_mode_label), style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { onToggleDark() },
                        thumbContent = {
                            if (darkMode) {
                                Icon(
                                    imageVector = Icons.Filled.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondaryContainer,
                            checkedTrackColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            checkedBorderColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            checkedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,

                            uncheckedThumbColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                            uncheckedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                            uncheckedIconColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }

                Button(
                    onClick = { showBlockedDialog = true },
                    modifier = Modifier.fillMaxWidth(),

                ) { Text(text = stringResource(id = R.string.blocked_users)) }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.log_out), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (showBlockedDialog) {
            BlockedUsersDialog(onClose = { showBlockedDialog = false })
        }
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.confirmation)) },
                text = { Text(stringResource(R.string.logout_confirm_message)) },
                confirmButton = {
                    Button(onClick = {
                        showLogoutDialog = false
                        AuthRepository.clearToken()
                        navController.navigate("start")
                    }) {
                        Text(stringResource(R.string.logout_confirm_yes))
                    }
                },
                dismissButton = {
                    Button(onClick = { showLogoutDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun BlockedUsersDialog(
    onClose: () -> Unit,
    // todo: użyć w blockedusersdialog viewModel: FriendsViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<FriendItem>>(emptyList()) }
    var processingId by remember { mutableStateOf<String?>(null) }
    var myUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            UserRepository.fetchMyProfile().onSuccess { myUserId = it._id }
            FriendshipRepository.fetchBlocked()
                .onSuccess { items = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text(text = stringResource(id = R.string.close)) } },
        title = { Text(text = stringResource(id = R.string.blocked_users)) },
        text = {
            when {
                loading -> { CircularProgressIndicator() }
                error != null -> { Text(error ?: stringResource(id = R.string.error), color = MaterialTheme.colorScheme.error) }
                items.isEmpty() -> { Text(text = stringResource(id = R.string.no_blocked_users)) }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEach { u ->
                            val isProcessing = processingId == u.friendshipId
                            val showUnblock = myUserId != null && myUserId == u.blockedBy

                            if (showUnblock && !isProcessing) {
                                FriendCard(
                                    friend = u,
                                    modifier = Modifier.fillMaxWidth(),
                                    onCardClick = {},
                                    onUnblockClick = {
                                        scope.launch {
                                            processingId = u.friendshipId
                                            FriendshipRepository.unblockFriendship(u.friendshipId)
                                                .onSuccess {
                                                    items = items.filterNot { it.friendshipId == u.friendshipId }
                                                    FriendshipRepository.fetchBlocked().onSuccess { items = it }
                                                }
                                            processingId = null
                                        }
                                    }
                                )
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