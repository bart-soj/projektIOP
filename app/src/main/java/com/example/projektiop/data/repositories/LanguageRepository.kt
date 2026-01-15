package com.example.projektiop.data.repositories

import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.projektiop.domain.models.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LanguageRepository(private val sharedDataSource: SharedDataSource) {

    companion object {
        private const val KEY_LANGUAGE = "app_language"
    }

    private val _language = MutableStateFlow(getStoredLanguage())
    val language: StateFlow<Language> = _language.asStateFlow()

    init {
        applyLanguage(_language.value)
    }

    private fun getStoredLanguage(): Language {
        val tag = sharedDataSource.get(KEY_LANGUAGE, "")
        return Language.fromTag(tag).takeIf { it != Language.SYSTEM } ?: resolveSystemLanguage()
    }

    fun setLanguage(language: Language) {
        sharedDataSource.set(KEY_LANGUAGE, language.tag)
        _language.value = language
        applyLanguage(language)
    }

    private fun applyLanguage(language: Language) {
        val locale = when (language) {
            Language.SYSTEM -> Resources.getSystem().configuration.locales[0]
            else -> Locale(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
    }

    fun resolveSystemLanguage(): Language {
        val systemLocale = Resources.getSystem().configuration.locales[0]
        return when (systemLocale.language) {
            Language.POLISH.tag -> Language.POLISH
            else -> Language.ENGLISH
        }
    }
}

