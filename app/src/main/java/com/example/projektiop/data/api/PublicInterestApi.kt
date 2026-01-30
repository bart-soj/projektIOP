package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PublicInterestApi {
    @GET("public/interests")
    suspend fun getPublicInterests(
        @Query("lang") lang: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("name") name: String? = null
    ): Response<List<PublicInterestDto>>

    @GET("public/interests/categories")
    suspend fun getCategories(
        @Query("lang") lang: String? = null
    ): Response<List<PublicInterestCategoryDto>>
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
