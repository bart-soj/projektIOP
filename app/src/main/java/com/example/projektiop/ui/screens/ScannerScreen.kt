package com.example.projektiop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.shape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.projektiop.data.db.realm.objects.User
import com.example.projektiop.domain.models.FriendshipStatus
import com.example.projektiop.domain.models.Interest
import com.example.projektiop.domain.models.SearchProfile
import com.example.projektiop.ui.components.MultiSelectInterestsDropdown
import com.example.projektiop.ui.components.UserAvatar
import org.koin.androidx.compose.koinViewModel


private const val ID: String = "_id"
private const val BASE_URL_KEY: String = "BASE_URL"


@OptIn(ExperimentalMaterial3Api::class) // Dla Scaffold
@Composable
fun ScannerScreen( navController: NavController) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val viewModel = koinViewModel<ScannerViewModel>()
    var showSearchProfileDialog by remember { mutableStateOf(false) }

    val users by viewModel.users.collectAsState()

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

            BLEControls(viewModel, onSearchProfileClick = { showSearchProfileDialog = true })

            Spacer(modifier = Modifier.height(8.dp))
            when (showSearchProfileDialog) {
                true -> SearchProfileDialog(viewModel, onClose = { showSearchProfileDialog = false })
                false -> Column {
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

    Text(
        text = stringResource(R.string.ble_status_label, statusText),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}


@Composable
fun SearchProfileStatus(searchProfile: SearchProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = onClick,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        label = {
            Text(
                text = searchProfile.name,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 4.dp
                )
            )
        },
        modifier = modifier
    )
}


@Composable
fun BLEControls(viewModel: ScannerViewModel, onSearchProfileClick: () -> Unit, modifier: Modifier = Modifier) { // TODO() use switches
    val isScanning by viewModel.isScanning.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val searchProfile by viewModel.searchProfile.collectAsState()

    ScanStatus(isScanning, isAdvertising)

    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.SpaceBetween) {

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

        Spacer(modifier = Modifier.width(8.dp))

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
    }

    Spacer(modifier = Modifier.height(8.dp))

    SearchProfileStatus(searchProfile, onSearchProfileClick)
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
fun SearchProfileItem(searchProfile: SearchProfile, modifier: Modifier = Modifier,
                      onDeleteClick: (() -> Unit)? = null, onChooseClick: (() -> Unit)? = null) {
    var expaneded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(onClick = { expaneded = !expaneded }, modifier = Modifier.background(color = Color.Transparent)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
            ) {
                // name
                Text(searchProfile.name)

                Row() {
                    if (onChooseClick != null) {
                        IconButton(onClick = onChooseClick) {
                            Icon(
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = null
                            )
                        }
                    }
                    // delete button
                    if (onDeleteClick != null) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }

        if (expaneded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                searchProfile.interests.forEach { item ->
                    Text(
                        text = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProfileDialog(viewModel: ScannerViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {

    val searchProfileList by viewModel.searchProfileList.collectAsState()
    val errorMessage by viewModel.searchProfileError.collectAsState()
    val searchText by viewModel.searchSearchProfileText.collectAsState()
    val loading by viewModel.searchProfileLoading.collectAsState()
    val searchProfile by viewModel.searchProfile.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshSearchProfileList()
    }

    when (showAddDialog) {
        true -> { // TODO() better input validation and respond to errors/loading
            var name by remember { mutableStateOf("") }
            var selectedInterests by remember { mutableStateOf<Set<Interest>>(emptySet()) }
            val allInterests by viewModel.publicInterests.collectAsState()

            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                // save
                IconButton(onClick = { viewModel.addSearchProfile(name, selectedInterests)
                    showAddDialog = false },
                    enabled = name.isNotBlank() && !searchProfileList.map{it.name}.contains(name)
                ) { Icon(imageVector = Icons.Filled.Add, contentDescription = null) }
                // go back
                IconButton(onClick = {showAddDialog = false}
                ) { Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null) }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 50) name = it },
                label = { Text(stringResource(id = R.string.name_label)) },
                supportingText = { Text(stringResource(id = R.string.nickname_count, name.length)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            MultiSelectInterestsDropdown(
                all = allInterests,
                selected = selectedInterests,
                onChange = { set -> selectedInterests = set }
            )
        }
        false -> {

            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                // add
                IconButton(onClick = {
                    showAddDialog = true
                }) { Icon(imageVector = Icons.Filled.Add, contentDescription = null) }
                // refresh
                IconButton(
                    onClick = { viewModel.refreshSearchProfileList() }
                ) { Icon(imageVector = Icons.Filled.Refresh, contentDescription = null) }
                // go back
                IconButton(
                    onClick = onClose
                ) { Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null) }
            }

            Spacer(modifier = Modifier.width(8.dp))

            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { viewModel.searchForSearchProfile(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            when {
                loading -> {
                    Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                }

                errorMessage != null -> {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                errorMessage ?: stringResource(R.string.error),
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                viewModel.refreshSearchProfileList()
                            }) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }

                searchProfileList.isEmpty() -> {
                    SearchProfileItem(searchProfile,
                        modifier = Modifier.background(color = Color.Green).padding(horizontal = 16.dp))
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.no_search_profiles),
                            Modifier.align(Alignment.Center)
                        )
                    }
                }

                else -> {
                    SearchProfileItem(searchProfile,
                        modifier = Modifier.background(color = Color.Green).padding(horizontal = 16.dp))
                    LazyColumn(
                        modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchProfileList, key = { it.name }) { searchProfile ->
                            SearchProfileItem(
                                searchProfile,
                                onDeleteClick = { viewModel.deleteSearchProfile(searchProfile) },
                                onChooseClick = { viewModel.chooseSearchProfile(searchProfile) }
                            )
                        }
                    }
                }
            }
        }
    }
}

