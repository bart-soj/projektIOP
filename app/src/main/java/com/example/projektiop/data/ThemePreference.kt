package com.example.projektiop.data

import android.content.Context
import android.content.SharedPreferences

object ThemePreference {
    private const val PREFS = "theme_prefs"
    private const val KEY_DARK = "dark_mode"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun isDark(): Boolean = prefs?.getBoolean(KEY_DARK, false) ?: false

    fun setDark(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_DARK, enabled)?.apply()
    }
}
