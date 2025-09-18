package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R
import com.example.projektiop.data.repositories.FriendItem
import com.example.projektiop.data.repositories.PendingRequestItem
import com.example.projektiop.data.repositories.FriendshipRepository
import kotlinx.coroutines.launch
import com.example.projektiop.data.api.UserSearchDto
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projektiop.data.repositories.SharedPreferencesRepository

private const val BASE_URL_KEY: String = "BASE_URL"

// Ekran dynamiczny listy znajomych z API
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var friends by remember { mutableStateOf<List<FriendItem>>(emptyList()) }
    var incoming by remember { mutableStateOf<List<PendingRequestItem>>(emptyList()) }
    var lastIncomingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refreshAll() {
        scope.launch {
            loading = true
            error = null
            val fr = FriendshipRepository.fetchAccepted()
            val pend = FriendshipRepository.fetchIncomingPending()
            fr.onSuccess { friends = it }.onFailure { error = it.message }
            pend.onSuccess { list ->
                val newOnes = list.filter { it.friendshipId !in lastIncomingIds }
                if (newOnes.isNotEmpty()) {
                    newOnes.take(3).forEach { req ->
                        com.example.projektiop.util.NotificationHelper.notifyFriendRequest(context, req.displayName)
                    }
                }
                incoming = list
                lastIncomingIds = list.map { it.friendshipId }.toSet()
            }.onFailure { error = it.message }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refreshAll() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista znajomych") },
                actions = {
                    var showSearch by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Szukaj użytkowników")
                    }
                    if (showSearch) {
                        UserSearchDialog(onClose = { showSearch = false })
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
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error ?: "Błąd", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { refreshAll() }) { Text("Spróbuj ponownie") }
                    }
                }
                friends.isEmpty() && incoming.isEmpty() -> Text("Brak znajomych", modifier = Modifier.align(Alignment.Center))
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (incoming.isNotEmpty()) {
                            item("pending_header") {
                                Text("Oczekujące zaproszenia", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
                            }
                            items(incoming, key = { it.friendshipId }) { req ->
                                PendingRequestCard(
                                    item = req,
                                    onAccept = {
                                        scope.launch {
                                            FriendshipRepository.acceptFriendship(req.friendshipId).onSuccess { refreshAll() }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            FriendshipRepository.rejectFriendship(req.friendshipId).onSuccess { refreshAll() }
                                        }
                                    }
                                )
                            }
                            item("divider_after_pending") { Divider(Modifier.padding(vertical = 4.dp)) }
                        }
                        if (friends.isNotEmpty()) {
                            item("friends_header") {
                                Text("Znajomi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
                            }
                            items(friends, key = { it.id }) { friend ->
                                FriendCard(
                                    friend = friend,
                                    onClick = { navController.navigate("friend_profile/${friend.id}?username=${friend.username}&displayName=${friend.displayName}") },
                                    onChat = { navController.navigate("chat_detail?chatId=null&friendId=${friend.id}") },
                                    onRemoved = { refreshAll() },
                                    onBlocked = { refreshAll() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendItem,
    onClick: () -> Unit,
    onChat: () -> Unit,
    onRemoved: () -> Unit,
    onBlocked: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { menuExpanded = true },
                    onTap = { onClick() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rawUrl = friend.avatarUrl?.takeIf { it.isNotBlank() }
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
                    contentDescription = "Avatar",
                    placeholder = painterResource(R.drawable.avatar_placeholder),
                    error = painterResource(R.drawable.avatar_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
            }

            // Przykładowy przycisk akcji (np. czat)
            TextButton(onClick = onChat) { Text("Czat") }

            Box { // Dropdown anchor
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Usuń znajomego") },
                        onClick = {
                            menuExpanded = false
                            scope.launch {
                                FriendshipRepository.removeFriend(friend.friendshipId)
                                    .onSuccess { onRemoved() }
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Zablokuj") },
                        onClick = {
                            menuExpanded = false
                            scope.launch {
                                FriendshipRepository.blockFriendship(friend.friendshipId)
                                    .onSuccess { onBlocked() }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingRequestCard(item: PendingRequestItem, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rawUrl = item.avatarUrl?.takeIf { it.isNotBlank() }
            val fullUrl = rawUrl?.let { if (it.startsWith("http")) it else "https://hellobeacon.onrender.com$it" }
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
                    contentDescription = "Avatar",
                    placeholder = painterResource(R.drawable.avatar_placeholder),
                    error = painterResource(R.drawable.avatar_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                Text(item.username, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onReject) { Text("Odrzuć") }
                Button(onClick = onAccept) { Text("Akceptuj") }
            }
        }
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
            TextButton(onClick = onClose) { Text("Zamknij") }
        },
        title = { Text("Szukaj użytkowników") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Fraza (min 1 znak)") },
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
                    Text("Brak wyników", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                    ) {
                        items(results, key = { it._id ?: it.username ?: it.hashCode().toString() }) { user ->
                            val userId = user._id ?: return@items
                            val alreadySent = userId in sentFor
                            ListItem(
                                headlineContent = { Text(user.profile?.displayName ?: user.username ?: "(bez nazwy)") },
                                supportingContent = { Text(user.username ?: "") },
                                trailingContent = {
                                    TextButton(enabled = !alreadySent, onClick = {
                                        scope.launch {
                                            FriendshipRepository.sendFriendRequest(userId)
                                                .onSuccess { sentFor = sentFor + userId }
                                                .onFailure { error = it.message }
                                        }
                                    }) { Text(if (alreadySent) "Wysłano" else "Dodaj") }
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

@Preview(showBackground = true)
@Composable
fun FriendsListPreview() {
    FriendsListScreen(navController = rememberNavController())
}
