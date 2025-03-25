package com.example.projektiop

import android.os.Build
import android.Manifest
import BluetoothManagerUtils
import android.os.Bundle
import android.widget.Toast
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
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjektIOPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = (0..999999).random().toString(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        requestPermissions()
    }
    private fun requestPermissions() {
        // Deklaracja typu: MutableList<String>
        val permissions: MutableList<String> = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)

        // Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        // Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        // Filtrowanie uprawnień, które jeszcze nie zostały przyznane
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // Jeśli są uprawnienia do zażądania, to je prosimy
        if (permissionsToRequest.isNotEmpty()) {
            Log.d("PermissionRequest", "Requesting permissions: $permissionsToRequest")
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("PermissionRequest", "All permissions already granted.")
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions: Map<String, Boolean> ->
            for ((permission, isGranted) in permissions) {
                if (!isGranted) {
                    // Jeśli użytkownik odrzucił uprawnienie, możemy go o tym poinformować
                    showPermissionDeniedMessage(permission)
                }
            }
        }

    private fun showPermissionDeniedMessage(permission: String) {
        when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                // Możesz wyświetlić np. Toast lub AlertDialog
                Log.d("PermissionRequest", "Dostęp do lokalizacji został odrzucony!")
            }
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT -> {
                Log.d("PermissionRequest", "Uprawnienia Bluetooth zostały odrzucone!")
            }
            Manifest.permission.NEARBY_WIFI_DEVICES -> {
                Log.d("PermissionRequest", "Uprawnienia do urządzeń w pobliżu zostały odrzucone!")
            }
        }
    }
}

//Główny ekran
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bleManager = remember { BluetoothManagerUtils(context) }


    Column(modifier = modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)){
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
    }

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
        Greeting(12345.toString())
    }
}