package com.example.projektiop.data.repositories

import com.example.projektiop.data.SharedDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreference(private val sharedDataSource: SharedDataSource) {
    private val PREFS = "theme_prefs"
    private val KEY_DARK = "dark_mode"

    private val _isDark: MutableStateFlow<Boolean> = MutableStateFlow(isDark())
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    fun isDark(): Boolean = sharedDataSource.get(KEY_DARK, false)

    fun setDark(enabled: Boolean) {
        sharedDataSource.set(KEY_DARK, enabled)
        _isDark.value = enabled
    }
}