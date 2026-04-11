package com.example.projektiop.activeHandshake.NFC

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NFCVIewModel : ViewModel() {
    private val _loading = MutableStateFlow<Boolean>(true)
    val loading = _loading.asStateFlow()

    fun onBlueClick(context: Context) { // right
        launchActivity(context, ReadNFCActivity::class.java)
    }

    fun onGreenClick(context: Context) { // left
        launchActivity(context, HostBasedCardEmulatorActivity::class.java)
    }


    private fun launchActivity(context: Context, activityClass: Class<out Activity>) {
        val intent = Intent(context, activityClass)
        context.startActivity(intent)
        // can add animations in here
        // (context as? Activity)?.overridePendingTransition(
        //    R.anim.slide_in_right,
        //    R.anim.slide_out_left
        // )
    }
}