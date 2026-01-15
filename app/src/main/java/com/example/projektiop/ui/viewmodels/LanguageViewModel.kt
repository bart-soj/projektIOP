package com.example.projektiop.ui.viewmodels

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import com.example.projektiop.data.repositories.LanguageRepository
import com.example.projektiop.domain.models.Language

class LanguageViewModel(private val languageRepository: LanguageRepository) : ViewModel() {
    val language = languageRepository.language

    fun toggleLanguage() {
        val newLanguage = when (language.value) {
            Language.POLISH -> Language.ENGLISH
            else -> Language.POLISH
        }

        languageRepository.setLanguage(newLanguage)
    }
}