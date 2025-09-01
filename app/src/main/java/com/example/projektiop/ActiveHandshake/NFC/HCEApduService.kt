package com.example.projektiop.activeHandshake.NFC

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.nfc.NdefRecord
import android.nfc.NdefMessage
import java.nio.charset.StandardCharsets


class HCEApduService : HostApduService() {
    val NDEF_APPLICATION_AID = "FFAAFFAAFFAA"
    val SELECT_APDU_HEADER = "00A40400".toByteArray()
    val NDEF_DATA_FILE_ID = "E104".toByteArray()
    val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        // executed on main thread, must return immediately
        val ndefMessage = byteArrayOf(
            0xC2.toByte(), 0x0A, 0x0C,
            0x74, 0x65, 0x78, 0x74, 0x2F, 0x70, 0x6C, 0x61, 0x69, 0x6E,  // "text/plain"
            0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x2C, 0x20, 0x4E, 0x46, 0x43, 0x21  // "Hello, NFC!"
        )

        if (commandApdu.contentEquals(SELECT_APDU_HEADER + NDEF_APPLICATION_AID)) {
            return SW_SUCCESS
        } else {
            return ndefMessage
        }
    }

    override fun onDeactivated(reason: Int) {
    }
}
