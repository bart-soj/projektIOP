package com.example.projektiop.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
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

@Composable
fun SettingsScreen(navController: NavController) {

    var animationPlayed by remember { mutableStateOf(false) } // Flaga, by animacja zagrała raz
    val alphaAnimation = animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f, // Cel: 1 (widoczne) lub 0 (niewidocne)
        animationSpec = tween(durationMillis = 1000) // Czas trwania animacji (1 sekunda)
    )

    // Uruchom zmianę targetValue, gdy kompozycja się pojawi po raz pierwszy
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController, currentRoute = currentRoute) }
    ) { paddingValues ->
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
                // Przyciski (bez zmian w ich definicji)
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.start_screen_login_button)) }

                Button(
                    onClick = { navController.navigate("register") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.start_screen_register_button)) }

                Button(
                    onClick = { navController.navigate("scanner") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.start_screen_scanner_button)) }

                Button(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.start_screen_main_button)) }

                Button(
                    onClick = { navController.navigate("start") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start Screen") }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        AuthRepository.clearToken()
                        navController.navigate("start")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Wyloguj się", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    StartScreen(navController = NavController(LocalContext.current))
}