package com.example.projektiop

import BluetoothManagerUtils
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.projektiop.ui.theme.ProjektIOPTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.compose.ui.Alignment


class MainActivity : ComponentActivity() {

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach { (permission, granted) ->
                if (!granted) {
                    showPermissionDeniedMessage(permission)
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp()
        }
        requestBluetoothPermissions(this, permissionsLauncher)
    }

}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "start") {
        composable("start") { StartScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable ("scanner") {
            ScannerScreen(
                name = "brak",
                modifier = Modifier.padding(10.dp),
                navController = navController
        ) }
    }
}

@Composable
fun StartScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { navController.navigate("login") }, modifier = Modifier.padding(8.dp)) {
            Text("Zaloguj się")
        }
        Button(onClick = { navController.navigate("register") }, modifier = Modifier.padding(8.dp)) {
            Text("Zarejestruj się")
        }
        Button(onClick = { navController.navigate("scanner") }, modifier = Modifier.padding(8.dp)) {
            Text("Skaner")
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ekran logowania")
        Button(onClick = { navController.popBackStack() }, modifier = Modifier.padding(8.dp)) {
            Text("Powrót")
        }
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ekran rejestracji")
        Button(onClick = { navController.popBackStack() }, modifier = Modifier.padding(8.dp)) {
            Text("Powrót")
        }
    }
}

//Główny ekran
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

        Status()
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


//Tutaj pokazuj status czyli np polaczono z itp
@Composable
fun Status(text: String = "brak", modifier: Modifier = Modifier) {
    var importText by remember{
        mutableStateOf(text)
    }

    var statusText = "Status: "+ importText

    Text(text = statusText, modifier = modifier)
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProjektIOPTheme {
        ScannerScreen(12345.toString(), navController = rememberNavController())
    }
}