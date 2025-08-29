package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

// Prosty model danych dla szablonu listy znajomych
data class Friend(
    val id: String,
    val name: String,
    val avatarRes: Int = R.drawable.avatar_placeholder
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val sampleFriends = listOf(
        Friend(id = "1", name = "Alicja Nowak"),
        Friend(id = "2", name = "Bartek Kowalski"),
        Friend(id = "3", name = "Celina Wiśniewska"),
        Friend(id = "4", name = "Dominik Zieliński"),
        Friend(id = "5", name = "Ewa Malinowska")
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lista znajomych") })
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        // Treść: lista
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(sampleFriends) { friend ->
                FriendCard(friend = friend, onClick = {
                    // Na razie nie nawigujemy na szczegóły.
                    // Możesz tu dodać: navController.navigate("friend/${friend.id}")
                })
            }
        }
    }
}

@Composable
private fun FriendCard(friend: Friend, onClick: () -> Unit) {
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
                painter = painterResource(id = friend.avatarRes),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name, style = MaterialTheme.typography.titleMedium)
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
