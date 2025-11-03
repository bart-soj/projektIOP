package com.example.projektiop.data.api

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.DELETE

// Zakładany endpoint profilu zalogowanego użytkownika.
// Jeśli backend różni się ścieżką, zmień @GET("user/me") odpowiednio (np. "users/me" albo "profile/me").
interface UserApi {
    // server.js: app.use('/api/users', userRoutes) + userRoutes route '/profile' => pełny endpoint: /api/users/profile
    @GET("users/profile")
    suspend fun getMyProfile(): Response<UserProfileResponse>

    @PUT("users/profile")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): Response<UserProfileResponse>

    // Upload avatar image as multipart/form-data with field name 'avatarImage'
    @Multipart
    @PUT("users/profile/avatar")
    suspend fun uploadAvatar(@Part avatarImage: MultipartBody.Part): Response<UserProfileResponse>

    // TODO() THIS DOES NOT EXIST SERVERSIDE YET
    // Profil innego użytkownika po ID: GET /api/users/{id}
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<UserProfileResponse>

    // Wyszukiwanie użytkowników: GET /api/users/search?q=...
    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<UserSearchDto>>

    // Interests management
    @POST("users/profile/interests")
    suspend fun addUserInterest(@Body body: AddUserInterestRequest): Response<UserProfileResponse>

    @PUT("users/profile/interests/{userInterestId}")
    suspend fun updateUserInterest(
        @Path("userInterestId") userInterestId: String,
        @Body body: UpdateUserInterestRequest
    ): Response<UserProfileResponse>

    @DELETE("users/profile/interests/{userInterestId}")
    suspend fun removeUserInterest(@Path("userInterestId") userInterestId: String): Response<UserProfileResponse>
}

// Dane profilu – wszystkie pola opcjonalne, żeby uniknąć crashy przy różnym JSON.
data class UserProfileResponse(
    val profile: Profile? = Profile(),
    val _id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val role: String? = null,
    val isBanned: Boolean? = null,
    val banReason: String? = null,
    val bannedAt: String? = null,
    val isTestAccount: Boolean? = null,
    val isEmailVerified: Boolean? = null,
    val isDeleted: Boolean? = null,
    val deletedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val __v: Int? = null,
    val interests: List<UserInterestDto>? = emptyList()
) {
    val effectiveDisplayName: String?
        get() = profile?.displayName
    val effectiveDescription: String?
        get() = profile?.bio
}



data class UserInterestDto(
    val userInterestId: com.google.gson.JsonElement?,
    val interest: InterestDto,
    val customDescription: String? = null
)

data class InterestDto(
    val _id: String,                 // MongoDB ObjectId
    val name: String,
    val category: JsonElement? = null,
    val description: String? = null,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class Profile(
    val displayName: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val location: String? = null,
    val birthDate: String? = null,
    val broadcastMessage: String? = null,
    val avatarUrl: String? = null
)

data class UserSearchDto(
    val _id: String? = null,
    val username: String? = null,
    val profile: Profile? = null
)

data class UserStats(
    val friends: Int? = null,
    val posts: Int? = null,
    val likes: Int? = null
)

// Request do aktualizacji profilu – dopasowany do validatorów w userRoutes.js (profile.*)
data class UpdateProfileRequest(
    val profile: Profile
)

data class AddUserInterestRequest(
    val interestId: String,
    val customDescription: String? = null
)

data class UpdateUserInterestRequest(
    val customDescription: String? = null
)

/*
data class ProfilePatch(
    val displayName: String? = null,
    val gender: String? = null,
    val birthDate: String? = null, // ISO8601 jeśli użyte
    val location: String? = null,
    val bio: String? = null,
    val broadcastMessage: String? = null
)
*/