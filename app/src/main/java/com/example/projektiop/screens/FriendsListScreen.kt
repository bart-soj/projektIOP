package com.example.projektiop.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.projektiop.R
import com.example.projektiop.data.api.UserSearchDto
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.screens.components.FriendCard
import com.example.projektiop.screens.components.PendingRequestCard
import com.example.projektiop.screens.friends.FriendsUiEffect
import com.example.projektiop.screens.friends.FriendsViewModel
import com.example.projektiop.util.NotificationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    navController: NavController,
    viewModel: FriendsViewModel = viewModel()
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val uiState by viewModel.uiState.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var lastIncomingIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is FriendsUiEffect.NavigateToProfile -> navController.navigate(effect.route)
                is FriendsUiEffect.NavigateToChat -> navController.navigate("chat_detail?chatId=null&friendId=${effect.friendId}")
                is FriendsUiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState.incomingRequests) {
        val newIds = uiState.incomingRequests.map { it.friendshipId }.toSet()
        val newOnes = uiState.incomingRequests.filter { it.friendshipId !in lastIncomingIds }
        if (newOnes.isNotEmpty()) {
            newOnes.take(3).forEach { req ->
                NotificationHelper.notifyFriendRequest(context, req.displayName)
            }
        }
        lastIncomingIds = newIds
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.friends_list_title)) },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_users)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.error ?: "Błąd", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.refreshAll() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                uiState.friends.isEmpty() && uiState.incomingRequests.isEmpty() ->
                    Text(
                        stringResource(R.string.no_friends),
                        modifier = Modifier.align(Alignment.Center)
                    )
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (uiState.incomingRequests.isNotEmpty()) {
                            item("pending_header") {
                                Text(
                                    stringResource(R.string.pending_requests),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(uiState.incomingRequests, key = { it.friendshipId }) { req ->
                                PendingRequestCard(
                                    item = req,
                                    onAccept = { viewModel.onAcceptRequest(req.friendshipId) },
                                    onReject = { viewModel.onRejectRequest(req.friendshipId) }
                                )
                            }
                            item("divider_after_pending") { Divider(Modifier.padding(vertical = 4.dp)) }
                        }
                        if (uiState.friends.isNotEmpty()) {
                            item("friends_header") {
                                Text(
                                    stringResource(R.string.friends_header),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(uiState.friends, key = { it.id }) { friend ->
                                FriendCard(
                                    friend = friend,
                                    onCardClick = { viewModel.onFriendClicked(friend) },
                                    onChatClick = { viewModel.onChatClicked(friend.id) },
                                    onRemoveClick = { viewModel.onRemoveFriend(friend.friendshipId) },
                                    onBlockClick = { viewModel.onBlockFriend(friend.friendshipId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSearch) {
        UserSearchDialog(onClose = { showSearch = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSearchDialog(onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserSearchDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sentFor by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
        },
        title = { Text(stringResource(R.string.search_users)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_query_label)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(enabled = query.isNotBlank(), onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                results = emptyList()
                                FriendshipRepository.searchUsers(query)
                                    .onSuccess { results = it }
                                    .onFailure { error = it.message }
                                loading = false
                            }
                        }) { Icon(Icons.Default.Search, contentDescription = null) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                } else if (results.isEmpty()) {
                    Text(stringResource(R.string.no_results), style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                    ) {
                        items(results, key = { it._id ?: it.username ?: it.hashCode().toString() }) { user ->
                            val userId = user._id ?: return@items
                            val alreadySent = userId in sentFor
                            ListItem(
                                headlineContent = { Text(user.profile?.displayName ?: user.username ?: stringResource(R.string.no_name)) },
                                supportingContent = { Text(user.username ?: "") },
                                trailingContent = {
                                    TextButton(enabled = !alreadySent, onClick = {
                                        scope.launch {
                                            FriendshipRepository.sendFriendRequest(userId)
                                                .onSuccess { sentFor = sentFor + userId }
                                                .onFailure { error = it.message }
                                        }
                                    }) {
                                        Text(if (alreadySent) stringResource(R.string.sent) else stringResource(R.string.add))
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    )
}

