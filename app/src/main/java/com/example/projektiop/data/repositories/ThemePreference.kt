package com.example.projektiop.data.repositories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemePreference {
    private const val PREFS = "theme_prefs"
    private const val KEY_DARK = "dark_mode"

    private val _isDark: MutableStateFlow<Boolean> = MutableStateFlow(isDark())
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    fun isDark(): Boolean = SharedPreferencesRepository.get(KEY_DARK, false)

    fun setDark(enabled: Boolean) {
        SharedPreferencesRepository.set(KEY_DARK, enabled)
        _isDark.value = enabled
    }
}