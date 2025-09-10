package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.Path

// Zakładany endpoint profilu zalogowanego użytkownika.
// Jeśli backend różni się ścieżką, zmień @GET("user/me") odpowiednio (np. "users/me" albo "profile/me").
interface UserApi {
    // server.js: app.use('/api/users', userRoutes) + userRoutes route '/profile' => pełny endpoint: /api/users/profile
    @GET("users/profile")
    suspend fun getMyProfile(): Response<UserProfileResponse>

    @PUT("users/profile")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): Response<UserProfileResponse>

    // Profil innego użytkownika po ID: GET /api/users/{id}
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<UserProfileResponse>

    // Wyszukiwanie użytkowników: GET /api/users/search?q=...
    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<UserSearchDto>>
}

// Dane profilu – wszystkie pola opcjonalne, żeby uniknąć crashy przy różnym JSON.
data class UserProfileResponse(
    val profile: Profile? = Profile(),
    val _id: String,
    val username: String,
    val email: String,
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
    val userInterestId: String,
    val interest: InterestDto,
    val customDescription: String? = null
)

data class InterestDto(
    val _id: String,                 // MongoDB ObjectId
    val name: String,
    val category: String? = null,
    val description: String = "",
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
    val broadcastMessage: String? = null
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