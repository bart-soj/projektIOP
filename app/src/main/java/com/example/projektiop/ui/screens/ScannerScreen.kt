package com.example.projektiop.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.projektiop.ui.viewmodels.ScannerViewModel
import com.example.projektiop.ui.viewmodels.UserWithStatus
import com.example.projektiop.R
import com.example.projektiop.activeHandshake.NFC.ActiveHandshakeButton
import com.example.projektiop.domain.models.FriendshipStatus
import com.example.projektiop.domain.models.SearchProfile
import com.example.projektiop.domain.models.User
import com.example.projektiop.ui.components.BottomNavigationBar
import com.example.projektiop.ui.components.UserAvatar
import org.koin.androidx.compose.koinViewModel
import com.example.projektiop.ui.components.SearchProfileDialog


private const val ID: String = "_id"
private const val BASE_URL_KEY: String = "BASE_URL"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen( navController: NavController) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val viewModel = koinViewModel<ScannerViewModel>()
    var showSearchProfileDialog by remember { mutableStateOf(false) }

    val users by viewModel.usersFromRepo.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Wycentruj elementy
        ) {
            Text(
                text = stringResource(R.string.scanner_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            BLEControls(viewModel, onSearchProfileClick = { showSearchProfileDialog = true })

            Spacer(modifier = Modifier.height(8.dp))
            when (showSearchProfileDialog) {
                true -> SearchProfileDialog(viewModel, onClose = { showSearchProfileDialog = false })
                false -> Column {
                    ScannedUsersList(
                        users = users,
                        onUserClick = { user ->
                            navController.navigate(
                                "friend_profile/${user.id}?username=${user.username}&displayName=${user.profile.displayName}&avatarUrl=${user.profile.avatarUrl}"
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

                    // ActiveHandshakeButton { }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun ScanStatus(isScanning: Boolean, isAdvertising: Boolean, modifier: Modifier = Modifier) {

    val statusText = when {
        isScanning && isAdvertising -> stringResource(R.string.ble_status_scanning_and_advertising)
        isScanning -> stringResource(R.string.ble_status_scanning)
        isAdvertising -> stringResource(R.string.ble_status_advertising)
        else -> stringResource(R.string.ble_status_inactive)
    }

    Row(horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ble_status_label),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium
        )
    }


}


@Composable
fun SearchProfileStatus(searchProfile: SearchProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()) {

        Text(
            text = stringResource(R.string.search_profile),
            style = MaterialTheme.typography.bodyMedium
        )

        SuggestionChip(
            onClick = onClick,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            label = {
                Text(
                    text = searchProfile.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            modifier = modifier
        )
    }
}


@Composable
fun BLEControls(viewModel: ScannerViewModel, onSearchProfileClick: () -> Unit, modifier: Modifier = Modifier) {
    val isScanning by viewModel.isScanning.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val searchProfile by viewModel.searchProfile.collectAsState()

    Column {

        ScanStatus(isScanning, isAdvertising)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            if (!isScanning) {
                Text(
                    stringResource(R.string.scanner_start_scanning),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    stringResource(R.string.scanner_stop_scanning),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Switch(
                checked = isScanning,
                onCheckedChange = {
                    if (!isScanning) {
                        viewModel.startScan()
                    } else {
                        viewModel.stopScan()
                    }
                },
                thumbContent = {
                    if (isScanning) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            if (!isAdvertising) {
                Text(
                    stringResource(R.string.scanner_start_advertising),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    stringResource(R.string.scanner_stop_advertising),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Switch(
                checked = isAdvertising,
                onCheckedChange = {
                    if (!isAdvertising) {
                        viewModel.startAdvertising()
                    } else {
                        viewModel.stopAdvertising()
                    }
                },
                thumbContent = {
                    if (isAdvertising) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
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

        Spacer(modifier = Modifier.height(8.dp))

        SearchProfileStatus(searchProfile, onSearchProfileClick)
    }
}


@Composable
fun ScannedUserRow( // TODO() just use friend card
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
            UserAvatar(user.profile.avatarUrl, modifier = Modifier.size(56.dp) )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.profile?.displayName ?: user.username.toString(), style = MaterialTheme.typography.titleMedium)
                Text(user.username.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(user.email.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            when (status) {
                FriendshipStatus.NOT_FRIENDS-> Button(onClick = { onAddClick(user.id) }) { Text(stringResource(R.string.add)) }
                FriendshipStatus.PENDING -> Button(onClick = {}, enabled = false) { Text(stringResource(R.string.sent)) }
                FriendshipStatus.ACCEPTED -> Button(onClick = { onChatClick(user.id) }) {Text(stringResource(R.string.chat))}
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
fun SearchProfileItem(
    searchProfile: SearchProfile,
    modifier: Modifier = Modifier,
    // Added to allow proper Card tinting while keeping the shape
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onDeleteClick: (() -> Unit)? = null,
    onChooseClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // --- Header (Name + Actions) ---
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = searchProfile.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onChooseClick != null) {
                        IconButton(onClick = onChooseClick) {
                            Icon(imageVector = Icons.Filled.Wifi, contentDescription = null)
                        }
                    }
                    if (onDeleteClick != null) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                        }
                    }
                    // Visual cue for expansion
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.alpha(0.5f)
                    )
                }
            }

            // --- Expanded Content (Text below content) ---
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Column {
                    searchProfile.interests.forEach { item ->
                        Text(
                            text = item.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}