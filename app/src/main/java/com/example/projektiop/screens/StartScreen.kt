package com.example.projektiop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun StartScreen(navController: NavController) {
    Surface(
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { navController.navigate("login") },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Zaloguj się")
            }
            Button(
                onClick = { navController.navigate("register") },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Zarejestruj się")
            }
            Button(
                onClick = { navController.navigate("scanner") },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Skaner")
            }
        }
    }
}

@Preview
@Composable
fun StartScreenPreview() {
    StartScreen(navController = NavController(LocalContext.current))
}