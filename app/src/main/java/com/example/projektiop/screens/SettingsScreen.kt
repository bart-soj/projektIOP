package com.example.projektiop.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R

@Composable
fun SettingsScreen(navController: NavController) {

    var animationPlayed by remember { mutableStateOf(false) } // Flaga, by animacja zagrała raz
    val alphaAnimation = animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f, // Cel: 1 (widoczne) lub 0 (niewidoczne)
        animationSpec = tween(durationMillis = 1000) // Czas trwania animacji (1 sekunda)
    )

    // Uruchom zmianę targetValue, gdy kompozycja się pojawi po raz pierwszy
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp)
                // Zastosuj animowaną przezroczystość do całej kolumny
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
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    StartScreen(navController = NavController(LocalContext.current))
}