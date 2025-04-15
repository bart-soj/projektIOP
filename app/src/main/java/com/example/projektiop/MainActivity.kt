package com.example.projektiop // Upewnij się, że pakiet jest poprawny

import android.app.LauncherActivity
import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme // Dodano import dla stylu tekstu
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign // Dodano import dla wyrównania tekstu
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Ważny import
import com.example.projektiop.BluetoothLE.BLEActivity
import com.example.projektiop.BluetoothLE.BluetoothManagerUtils
import com.example.projektiop.activeHandshake.NFC.SimpleDistinguishableView
import com.example.projektiop.ui.theme.ProjektIOPTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intent = Intent(this, BLEActivity::class.java)
        startActivity(intent)
    }
}