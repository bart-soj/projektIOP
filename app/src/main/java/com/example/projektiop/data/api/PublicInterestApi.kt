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
    ): Response<List<InterestDto>>

    @GET("public/interests/categories")
    suspend fun getCategories(): Response<List<PublicInterestCategoryDto>>
}

data class PublicInterestCategoryDto(
    val _id: String,
    val name: String
)
