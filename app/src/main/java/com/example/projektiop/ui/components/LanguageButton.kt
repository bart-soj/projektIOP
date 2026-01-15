package com.example.projektiop.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.projektiop.R
import com.example.projektiop.domain.models.Language
import com.example.projektiop.ui.viewmodels.LanguageViewModel

@Composable
fun LanguageButton(
    viewModel: LanguageViewModel
) {
    val language by viewModel.language.collectAsState()

    val flagRes = when (language) {
        Language.POLISH -> R.drawable.ic_flag_pl
        else -> R.drawable.ic_flag_gb
    }

    IconButton(onClick = { viewModel.toggleLanguage() }) {
        Icon(
            painterResource(flagRes),
            contentDescription = null,
            tint = Color.Unspecified
        )
    }
}