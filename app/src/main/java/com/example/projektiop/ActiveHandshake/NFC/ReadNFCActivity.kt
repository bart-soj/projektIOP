package com.example.projektiop.activeHandshake.NFC

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.getDefaultAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.nfc.tech.IsoDep
import android.nfc.tech.TagTechnology
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.projektiop.ui.theme.ProjektIOPTheme
import java.nio.charset.Charset

class ReadNFCActivity : ComponentActivity() {
    lateinit var adapter : NfcAdapter
    // for foreground dispatch system
    lateinit var myintent: Intent
    lateinit var pendingIntent: PendingIntent
    val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
        try {
            addDataType("*/*")    /* Handles all MIME based dispatches.
                                 You should specify only the ones that you need. */
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }
    }

    val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
    var intentFiltersArray = arrayOf(tagDetected)
    var techListsArray = arrayOf(
        arrayOf(IsoDep::class.java.name),
        arrayOf("android.nfc.tech.NfcA"),
        arrayOf("android.nfc.tech.NfcB"),
        arrayOf("android.nfc.tech.NfcF"),
        arrayOf("android.nfc.tech.NfcV"),
        arrayOf("android.nfc.tech.Ndef"),
        arrayOf("android.nfc.tech.MifareClassic"),
        arrayOf("android.nfc.tech.MifareUltralight")
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = getDefaultAdapter(this)
        myintent = Intent(this, javaClass).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        pendingIntent = PendingIntent.getActivity(this, 0, myintent,
            PendingIntent.FLAG_MUTABLE)
        enableEdgeToEdge()
        setContent {
            ProjektIOPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SimpleDistinguishableView2 (
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        // adapter.disableForegroundDispatch(this)
        adapter.disableReaderMode(this)
    }

    public override fun onResume() {
        super.onResume()
        val options = Bundle()
        // adapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
        adapter.enableReaderMode(this, { tag ->
            Log.d("READ_NFC", "Detected tag via ReaderMode: $tag")
            // You can also check if it's IsoDep (usually used for HCE)
            val isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                Log.d("READ_NFC", "This tag uses IsoDep (likely HCE)")
            }
        },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            options
        )
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tagFromIntent: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
        // do something with tagFromIntent
        Log.d("READ_NFC", "GOTCHA!")

    }

    private fun initNfcExchange(tag: android.nfc.Tag) {
        val isoDep = IsoDep.get(tag) ?: return  // Get the IsoDep interface for the tag
        isoDep.connect()

        val apduCommand = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x06,  // SELECT command header
            0xFF.toByte(), 0xAA.toByte(), 0xFF.toByte(), 0xAA.toByte(), 0xFF.toByte(), 0xAA.toByte(),  // AID (Application ID)
            0x00  // Le (expected response length)
        )

        val response = isoDep.transceive(apduCommand)  // Send APDU command
        println("Received response: ${response.joinToString(" ")}")

        isoDep.close()
    }
}

@Composable
fun SimpleDistinguishableView2(
    modifier: Modifier = Modifier
) {
    Surface (
        modifier = modifier
            .fillMaxSize()
    ) {
        Box {
            Text(text = "RNFCA")
        }
    }
}