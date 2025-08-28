package com.example.projektiop.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun FriendsListScreen(navController: NavController) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text("Lista znajomych", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            // Szablon listy znajomych
            Text("Tu pojawi się lista Twoich znajomych.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
