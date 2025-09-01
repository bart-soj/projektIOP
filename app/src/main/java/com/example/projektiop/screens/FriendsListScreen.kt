package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R
import com.example.projektiop.data.FriendItem
import com.example.projektiop.data.FriendshipRepository
import kotlinx.coroutines.launch

// Ekran dynamiczny listy znajomych z API

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var friends by remember { mutableStateOf<List<FriendItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            error = null
            FriendshipRepository.fetchAccepted()
                .onSuccess { friends = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lista znajomych") })
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
                        Button(onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                FriendshipRepository.fetchAccepted()
                                    .onSuccess { friends = it }
                                    .onFailure { error = it.message }
                                loading = false
                            }
                        }) { Text("Spróbuj ponownie") }
                    }
                }
                friends.isEmpty() -> {
                    Text("Brak znajomych", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(friends, key = { it.id }) { friend ->
                            FriendCard(friend = friend, onClick = { /* TODO: nawigacja do czatu / profilu */ })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendCard(friend: FriendItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.avatar_placeholder), // TODO: załaduj avatarUrl jeśli dostępny
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
            }

            // Przykładowy przycisk akcji (np. czat)
            TextButton(onClick = { /* TODO: rozpocznij czat */ }) {
                Text("Czat")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsListPreview() {
    FriendsListScreen(navController = rememberNavController())
}
