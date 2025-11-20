package com.example.projektiop.data.repositories

import android.content.Context
import coil.network.HttpException
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.RegisterRequest
import com.example.projektiop.data.api.LoginRequest
import com.example.projektiop.data.api.AuthFailedDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor


object AuthRepository {
    private const val KEY_TOKEN = "auth_token"
    private const val EMAIL = "my_email"
    private const val KEY_REMEMBER = "remember_me"

    private var token: String? = null
    private var rememberMe: Boolean = false

    fun init(context: Context) {
        // Load persisted values directly from repository
        rememberMe = SharedPreferencesRepository.get(KEY_REMEMBER, false)

        if (rememberMe) {
            token = SharedPreferencesRepository.get(KEY_TOKEN, null)
        }
    }


    suspend fun login(email: String, password: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.authApi.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    saveInfo(email, response.body()?._id)
                    Result.success(response.body()?.token)
                } else {
                    Result.failure(Exception(""))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun register(username: String, email: String, password: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.authApi.register(RegisterRequest(username, email, password))
                if (response.isSuccessful) {
                    saveInfo(email, response.body()?._id)
                    Result.success(response.body()?.token)
                } else {
                    val converter = RetrofitInstance.errorConverter<AuthFailedDto>(AuthFailedDto::class.java)
                    val errorDto = converter.convert(response.errorBody()!!)
                    val errorMessage = errorDto?.message
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    fun saveToken(newToken: String?, remember: Boolean) {
        token = newToken
        rememberMe = remember

        if (remember && !newToken.isNullOrBlank()) {
            SharedPreferencesRepository.set(KEY_TOKEN, newToken!!)
            SharedPreferencesRepository.set(KEY_REMEMBER, true)
        } else {
            SharedPreferencesRepository.set(KEY_REMEMBER, false)
            SharedPreferencesRepository.remove(KEY_TOKEN)
        }
    }


    fun saveInfo(newEmail: String?, newId: String?) {
        if (!newEmail.isNullOrBlank()) {
            SharedPreferencesRepository.set(EMAIL, newEmail)
        }
        if (!newId.isNullOrBlank()) {
            SharedPreferencesRepository.set("_id", newId)
        }
    }


    fun clearToken() {
        token = null
        rememberMe = false
        SharedPreferencesRepository.set(KEY_REMEMBER, false)
        SharedPreferencesRepository.remove(KEY_TOKEN)
    }


    fun getToken(): String? = token
    fun isRemembered(): Boolean = rememberMe
}
