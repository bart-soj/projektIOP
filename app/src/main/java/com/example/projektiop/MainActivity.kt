package com.example.projektiop

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