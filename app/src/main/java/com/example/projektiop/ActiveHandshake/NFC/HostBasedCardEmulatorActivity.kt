package com.example.projektiop.activeHandshake.NFC

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.nfc.cardemulation.CardEmulation
import android.nfc.cardemulation.HostApduService
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
// import com.example.projektiop.Greeting
import com.example.projektiop.ui.theme.ProjektIOPTheme


class HostBasedCardEmulatorActivity : ComponentActivity() {
    private var serviceBound = false
    private var hceService: HostApduService? = null
    lateinit var cardEmulation: CardEmulation
    lateinit var nfcAdapter: NfcAdapter
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("HBCEA", "Apdu Service Connected")
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("HBCEA", "Apdu Service Disconnected")
            serviceBound = false
            hceService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        cardEmulation = CardEmulation.getInstance(nfcAdapter)
        Log.d("custom", "here")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startIntent = Intent(Intent.ACTION_RUN).apply {
            component = ComponentName("com.example.projektiop", "com.example.projektiop.activeHandshake.NFC.HCEApduService")
        }
        setContent {
            ProjektIOPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SimpleDistinguishableView (
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindHCEApduService()
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    public override fun onPause() {
        super.onPause()
        var unset : Boolean = cardEmulation.unsetPreferredService(this)
    }

    public override fun onResume() {
        super.onResume()
        var set : Boolean = cardEmulation.setPreferredService(this, ComponentName(this, ".activeHandshake.NFC.HCEApduService"))
    }

    private fun bindHCEApduService() {
        val intent = Intent(Intent.ACTION_RUN).apply {
            component = ComponentName("com.example.projektiop", "com.example.projektiop.activeHandshake.NFC.HCEApduService")
        }
        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}

@Composable
fun SimpleDistinguishableView(
    modifier: Modifier = Modifier
) {
    Surface (
        modifier = modifier
            .fillMaxSize()
            .background(Color.Green)
    ) {
        Box {
            Text(text = "HBCEA")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProjektIOPTheme {
        SimpleDistinguishableView()
    }
}
