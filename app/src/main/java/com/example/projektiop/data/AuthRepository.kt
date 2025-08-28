package com.example.projektiop.data

import com.example.projektiop.api.AuthApi
import com.example.projektiop.api.RetrofitInstance
import com.example.projektiop.api.RegisterRequest
import com.example.projektiop.api.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuthRepository {
    private var token: String? = null

    // Przykładowa funkcja logowania
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

    // Przykładowa funkcja rejestracji
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

    fun saveToken(newToken: String?) {
        token = newToken
        // Możesz dodać zapis do SharedPreferences jeśli chcesz trwałość
    }

    fun clearToken() {
        token = null
        // Jeśli używasz SharedPreferences, usuń zapisany token
    }

    fun getToken(): String? = token
}
