package com.example.projektiop.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Request/response models for certificate issuance
data class CertificateRequest(
    val csrPem: String
)

data class CertificateResponse(
    val certPem: String
)

// Retrofit API definition for certificate endpoints
interface CertificateApi {
    @POST("certificates/issue")
    suspend fun issueCertificate(
        @Header("Authorization") token: String,
        @Body request: CertificateRequest
    ): CertificateResponse
}
