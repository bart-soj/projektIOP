package com.example.projektiop.data.api

import com.example.projektiop.domain.models.BackupInfo
import com.example.projektiop.domain.models.PasswordDerivationParams
import com.example.projektiop.domain.models.base64
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BackupApi {
    @POST("backups")
    suspend fun saveBackup(@Body body: BackupInfo): Response<Unit>
    @GET("backups")
    suspend fun getBackup(): Response<BackupInfo>
}



