package com.example.projektiop.data.api

import com.example.projektiop.domain.models.base64
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PublicKeyApi {

    @POST("/api/keys/publish")
    suspend fun publishPublicKey(
        @Body request: base64
    ): Response<PublishPublicKeyResponse>

    @GET("/api/keys/{userId}")
    suspend fun getPublicKey(
        @Path("userId") userId: String
    ): Response<PublicKeyResponse>
}


data class PublishPublicKeyRequest(
    val publicKey: base64
)

data class PublishPublicKeyResponse(
    val message: String
)

data class PublicKeyResponse(
    val userId: String,
    val username: String,
    val displayName: String?,
    val publicKey: base64
)
