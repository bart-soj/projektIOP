package com.example.projektiop.data.api

import com.google.gson.JsonElement

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Public interests browsing API
interface PublicInterestApi {
    @GET("public/interests")
    suspend fun getPublicInterests(
        @Query("categoryId") categoryId: String? = null,
        @Query("name") name: String? = null
    ): Response<List<PublicInterestDto>>

    @GET("public/interests/categories")
    suspend fun getCategories(): Response<List<PublicInterestCategoryDto>>
}

data class PublicInterestDto(
    val _id: String? = null,
    val name: String? = null,
    val category: PublicInterestCategoryDto? = null,
    val description: String? = null,
    val isArchived: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class PublicInterestCategoryDto(
    val _id: String? = null,
    val name: String? = null
)
