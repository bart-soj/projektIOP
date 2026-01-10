package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/resend-verification")
    suspend fun resendVerificationEmail(
        @Body request: EmailRequest
    ): Response<MessageResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: EmailRequest
    ): Response<MessageResponse>
}


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

data class EmailRequest(
    val email: String
)

data class MessageResponse(
    val message: String
)


