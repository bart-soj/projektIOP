package com.example.projektiop.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.projektiop.R
import com.example.projektiop.screens.components.FriendCard
import com.example.projektiop.ui.components.BottomNavigationBar
import com.example.projektiop.ui.components.LanguageButton
import com.example.projektiop.ui.viewmodels.BackupViewModel
import com.example.projektiop.ui.viewmodels.LanguageViewModel
import com.example.projektiop.ui.viewmodels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.lazy.items

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val backupViewModel = koinViewModel<BackupViewModel>()
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
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { paddingValues ->
        var showLogoutDialog by remember { mutableStateOf(false) }
        var showBlockedDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        val rememberMe by viewModel.rememberMe.collectAsState()

        Box(modifier = Modifier.padding(paddingValues)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 32.dp)
                    .graphicsLayer { alpha = alphaAnimation.value },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val languageViewModel = koinViewModel<LanguageViewModel>()
                    LanguageButton(languageViewModel)
                }

                // light/dark mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = R.string.dark_mode_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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


                // remember me switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.remember_me),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = rememberMe,
                        onCheckedChange = { viewModel.rememberMe() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondaryContainer,
                            checkedTrackColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            checkedBorderColor = MaterialTheme.colorScheme.onSecondaryContainer,

                            uncheckedThumbColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                            uncheckedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.blocked_users),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { showBlockedDialog = true },
                    ) {
                        Icon(
                            Icons.Filled.LockPerson,
                            contentDescription = null
                        )
                    }
                }

                // Backup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isBackedUp = backupViewModel.isBackedUp
                    Text(
                        text = if (isBackedUp) stringResource(R.string.backed_up)
                        else stringResource(R.string.create_backup),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { navController.navigate("backup") },
                        enabled = !isBackedUp,
                        shape = RoundedCornerShape(4.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFFFFFFFF),
                            disabledContainerColor = Color(0xFF3c8c40),
                            disabledContentColor = Color(0xFFFFFFFF),
                        )
                    ) {
                        Icon(
                            if (!isBackedUp) Icons.Filled.Backup
                            else Icons.Filled.CloudDone,
                            contentDescription = null
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.delete_account),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        shape = RoundedCornerShape(4.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {


                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.log_out),
                            modifier = Modifier.size(32.dp)
                        )
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
                        if (backupViewModel.isBackedUp) {
                            Text(stringResource(R.string.logout_confirm_message))
                        } else {
                            Text(
                                stringResource(R.string.logout_confirm_message)
                                        + "\n" + stringResource(R.string.no_backup),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
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
                )
            }
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
}

@Composable
private fun BlockedUsersDialog(
    onClose: () -> Unit,
    viewModel: SettingsViewModel
) {
    var loading by remember { mutableStateOf(false) }

    val processingIds by viewModel.processingIds.collectAsStateWithLifecycle()
    val blocked by viewModel.blockedFriendItems.collectAsStateWithLifecycle()
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
                blocked.isEmpty() -> { Text(text = stringResource(id = R.string.no_blocked_users)) }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(blocked, key = { it.id }) { u ->
                            val showUnblock = myUserId != null && myUserId == u.blockedBy

                            if (showUnblock) {
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

