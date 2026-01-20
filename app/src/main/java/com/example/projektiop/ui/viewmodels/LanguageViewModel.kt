package com.example.projektiop.ui.viewmodels

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.LanguageRepository
import com.example.projektiop.domain.models.Language
import kotlinx.coroutines.launch

class LanguageViewModel(private val languageRepository: LanguageRepository, private val interestRepository: InterestRepository) : ViewModel() {
    val language = languageRepository.language

    fun toggleLanguage() {
        viewModelScope.launch {
            val newLanguage = when (language.value) {
                Language.POLISH -> Language.ENGLISH
                else -> Language.POLISH
            }

            languageRepository.setLanguage(newLanguage)
            interestRepository.getPublicInterests()
        }
    }
}