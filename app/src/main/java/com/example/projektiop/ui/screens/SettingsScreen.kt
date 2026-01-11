package com.example.projektiop.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
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
import com.example.projektiop.data.repositories.ThemePreference
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.screens.components.FriendCard
import com.example.projektiop.ui.components.BackupDialogButton
import com.example.projektiop.ui.theme.ProjektIOPTheme
import com.example.projektiop.ui.viewmodels.AuthViewModel
import com.example.projektiop.ui.viewmodels.BackupDialogViewModel
import com.example.projektiop.ui.viewmodels.SettingsViewModel
import com.example.projektiop.util.DataError
import com.example.projektiop.util.Result
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val backupDialogViewModel = koinViewModel<BackupDialogViewModel>()
    val viewModel = koinViewModel<SettingsViewModel>()
    var animationPlayed by remember { mutableStateOf(false) }
    val alphaAnimation = animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )
    val darkMode by viewModel.darkMode.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController, currentRoute = currentRoute) }
    ) { paddingValues ->

        var showLogoutDialog by remember { mutableStateOf(false) }
        var showBlockedDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

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
                        onCheckedChange = { viewModel.onToggleDark() },
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

                // Backup
                BackupDialogButton(modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.size(8.dp))

                Button(
                    onClick = { showBlockedDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )

                ) {
                    Text(text = stringResource(id = R.string.blocked_users))
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete_account), style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.size(8.dp))

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
            BlockedUsersDialog(onClose = { showBlockedDialog = false }, viewModel)
        }
        if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.confirmation)) },
            text = {
                if (backupDialogViewModel.isBackedUp) {
                    Text(stringResource(R.string.logout_confirm_message))
                } else {
                    Text(stringResource(R.string.logout_confirm_message)
                            + "\n" + stringResource(R.string.no_backup),
                        color = MaterialTheme.colorScheme.error )
                }},
            confirmButton = {
                Button(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) {
                    Text(stringResource(R.string.logout_confirm_yes))
                }
            },
            dismissButton = {
                Button(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )}
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.confirmation)) },
                text = { Text(stringResource(R.string.delete_account_warning)) },
                confirmButton = {
                    Button(onClick = {
                        showDeleteDialog = false
                        viewModel.onDeleteAccountClick()
                    }) {
                        Text(stringResource(R.string.delete_confirm_yes))
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) {
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
    viewModel: SettingsViewModel
) {
    var loading by remember { mutableStateOf(false) }

    val processingIds by viewModel.processingIds.collectAsState()
    val items by viewModel.blockedFriendItems.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val myUserId by viewModel.myUserId.collectAsState()

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(id = R.string.close))
            } },
        title = { Text(text = stringResource(id = R.string.blocked_users)) },
        text = {
            when {
                loading -> { CircularProgressIndicator() }
                error != null -> { Text(error ?: stringResource(id = R.string.error), color = MaterialTheme.colorScheme.error) }
                items.isEmpty() -> { Text(text = stringResource(id = R.string.no_blocked_users)) }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEach { u ->
                            val showUnblock = myUserId != null && myUserId == u.blockedBy

                            if (showUnblock && u.id !in processingIds) {
                                FriendCard(
                                    friend = u,
                                    modifier = Modifier.fillMaxWidth(),
                                    onCardClick = {},
                                    onUnblockClick = {
                                        viewModel.onUnblockClick(u.id, u.friendshipId)
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