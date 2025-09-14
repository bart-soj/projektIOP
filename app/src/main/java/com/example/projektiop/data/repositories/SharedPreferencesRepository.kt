package com.example.projektiop.data.repositories

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesRepository {
    private lateinit var appContext: Context
    private const val PREFS_NAME = "HelloBeaconSharedPrefs"

    fun init(context: Context) {
        appContext = context
    }

    fun getAppContext(): Context {
        return appContext
    }

    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun <T> set(key: String, value: T, context: Context  = this.getAppContext()) {
        with(getPreferences(context).edit()) {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("Unsupported type: ${value!!::class.java}")
            }
            apply()
        }
    }

    // @Suppress("UNCHECKED_CAST")
    inline fun <reified T> get(key: String, defaultValue: T, context: Context = this.getAppContext()): T {
        val prefs = getPreferences(context)
        return when (T::class) {
            String::class -> prefs.getString(key, defaultValue as? String ?: "") as T
            Int::class -> prefs.getInt(key, defaultValue as? Int ?: 0) as T
            Boolean::class -> prefs.getBoolean(key, defaultValue as? Boolean ?: false) as T
            Float::class -> prefs.getFloat(key, defaultValue as? Float ?: 0f) as T
            Long::class -> prefs.getLong(key, defaultValue as? Long ?: 0L) as T
            else -> throw IllegalArgumentException("Unsupported type: ${T::class.java}")
        }
    }

    fun remove(key: String, context: Context = this.getAppContext()) {
        getPreferences(context).edit().remove(key).apply()
    }
}

