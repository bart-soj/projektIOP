package com.example.projektiop.data

import android.content.Context
import android.content.SharedPreferences
import com.example.projektiop.data.repositories.SharedPreferencesRepository

object ThemePreference {
    private const val PREFS = "theme_prefs"
    private const val KEY_DARK = "dark_mode"

    fun isDark(): Boolean = SharedPreferencesRepository.get(KEY_DARK, false)

    fun setDark(enabled: Boolean) {
        SharedPreferencesRepository.set(KEY_DARK, true)
    }
}
