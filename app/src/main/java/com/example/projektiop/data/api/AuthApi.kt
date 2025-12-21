package com.example.projektiop.data.api

import android.health.connect.datatypes.units.Length
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Data classes for requests and responses


data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val _id: String?,
    val username: String?,
    val email: String?,
    val profile: ProfileDto?,
    val role: String?,
    val isTestAccount: String?,
    val token: String?,
    val isBackedUp: Boolean?
)

data class AuthFailedDto(
    @SerializedName("message")
    val message: String?,
)

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}
