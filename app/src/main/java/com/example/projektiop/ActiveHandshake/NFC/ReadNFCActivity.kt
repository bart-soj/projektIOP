package com.example.projektiop.activeHandshake.NFC

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.getDefaultAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable

class ReadNFCActivity : ComponentActivity() {
    val adapter : NfcAdapter = getDefaultAdapter(this)
    // for foreground dispatch system
    val myintent: Intent = Intent(this, javaClass).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
    var pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, myintent,
        PendingIntent.FLAG_MUTABLE)
    val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
        try {
            addDataType("*/*")    /* Handles all MIME based dispatches.
                                 You should specify only the ones that you need. */
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }
    }

    var intentFiltersArray = arrayOf(ndef)
    var techListsArray = arrayOf(arrayOf<String>(NfcF::class.java.name))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

        }
    }

    public override fun onPause() {
        super.onPause()
        adapter.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        adapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tagFromIntent: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
        // do something with tagFromIntent

    }
}