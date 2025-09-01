package com.example.projektiop.data

import android.content.Context
import android.content.SharedPreferences
import com.example.projektiop.api.RetrofitInstance
import com.example.projektiop.api.RegisterRequest
import com.example.projektiop.api.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuthRepository {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_REMEMBER = "remember_me"

    private var token: String? = null
    private var rememberMe: Boolean = false
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Load persisted values
            rememberMe = prefs?.getBoolean(KEY_REMEMBER, false) ?: false
            if (rememberMe) {
                token = prefs?.getString(KEY_TOKEN, null)
            }
        }
    }

    suspend fun login(email: String, password: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    Result.success(response.body()?.token)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Login failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.register(RegisterRequest(username, email, password))
                if (response.isSuccessful) {
                    Result.success(response.body()?.token)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Registration failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun saveToken(newToken: String?, remember: Boolean) {
        token = newToken
        rememberMe = remember
        prefs?.edit()?.apply {
            if (remember && !newToken.isNullOrBlank()) {
                putString(KEY_TOKEN, newToken)
                putBoolean(KEY_REMEMBER, true)
            } else {
                remove(KEY_TOKEN)
                putBoolean(KEY_REMEMBER, false)
            }
            apply()
        }
    }

    // Backwards compatibility for old calls (default remember = false)
    fun saveToken(newToken: String?) = saveToken(newToken, remember = false)

    fun clearToken() {
        token = null
        rememberMe = false
        prefs?.edit()?.apply {
            remove(KEY_TOKEN)
            putBoolean(KEY_REMEMBER, false)
            apply()
        }
    }

    fun getToken(): String? = token
    fun isRemembered(): Boolean = rememberMe
}
