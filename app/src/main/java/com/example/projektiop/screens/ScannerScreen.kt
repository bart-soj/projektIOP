package com.example.projektiop.screens

import BluetoothManagerUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ScannerScreen(name: String, modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    val bleManager = remember { BluetoothManagerUtils(context) }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text(text = "Your name is $name!", modifier = modifier)

        //przyciski
        Button(onClick = { bleManager.startScan() }) {
            Text("Rozpocznij skanowanie")
        }
        Button(onClick = { bleManager.stopScan() }) {
            Text("Zatrzymaj skanowanie")
        }
        Button(onClick = { bleManager.startAdvertising() }) {
            Text("Rozpocznij rozgłaszanie")
        }
        Button(onClick = { bleManager.stopAdvertising() }) {
            Text("Zatrzymaj Rozgłaszanie")
        }

        ScanStatus(bleManager)

        Button(onClick = { navController.popBackStack() }, modifier = Modifier.padding(8.dp)) {
            Text("Powrót")
        }
    }

}


@Composable
fun ScanStatus(bleManager: BluetoothManagerUtils, modifier: Modifier = Modifier) {
    val isScanning by bleManager.isScanning
    val isAdvertising by bleManager.isAdvertising

    val statusText = when {
        isScanning && isAdvertising -> "Skanowanie i Rozgłaszanie aktywne"
        isScanning -> "Skanowanie aktywne"
        isAdvertising -> "Rozgłaszanie aktywne"
        else -> "Nieaktywne"
    }

    Text(text = "Status BLE: $statusText", modifier = modifier)
}