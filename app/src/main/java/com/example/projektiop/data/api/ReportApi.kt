package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportApi {

    @POST("/api/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): Response<CreateReportResponse>
}


data class CreateReportRequest (
    val reportedUserId: String? = null,
    val reportedMessageId: String? = null,
    val reportType: String,
    val reason: String
)


data class CreateReportResponse(
    val message: String?,
    val report: ReportDto?
)


data class ReportDto(
    val _id: String?,
    val reportedBy: String?,
    val reportedUser: String?,
    val reportedMessage: String?,
    val reportType: String?,
    val reason: String?,
    val createdAt: String?,
    val updatedAt: String?
)