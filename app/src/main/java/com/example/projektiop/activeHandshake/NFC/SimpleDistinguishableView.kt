package com.example.projektiop.activeHandshake.NFC

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface


@Composable
fun SimpleDistinguishableView(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface (
        modifier = modifier
            .fillMaxSize()
    ) {
        Box {
            Text(text = text)
        }
    }
}