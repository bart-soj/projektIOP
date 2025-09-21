package com.example.projektiop.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Data classes for the API request/response
data class CertificateRequest(
    val csrPem: String
)

data class CertificateResponse(
    val certPem: String,
    val caCertPem: String
)

// Retrofit interface
interface CertificateApi {
    @POST("certificates/issue")
    suspend fun issueCertificate(
        @Header("Authorization") token: String,
        @Body request: CertificateRequest
    ): CertificateResponse
}
