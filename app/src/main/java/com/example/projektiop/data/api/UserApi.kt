package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName

// Zakładany endpoint profilu zalogowanego użytkownika.
// Jeśli backend różni się ścieżką, zmień @GET("user/me") odpowiednio (np. "users/me" albo "profile/me").
interface UserApi {
    // server.js: app.use('/api/users', userRoutes) + userRoutes route '/profile' => pełny endpoint: /api/users/profile
    @GET("users/profile")
    suspend fun getMyProfile(): Response<UserProfileResponse>

    @PUT("users/profile")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): Response<UserProfileResponse>

    // Wyszukiwanie użytkowników: GET /api/users/search?q=...
    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<UserSearchDto>>
}

// Dane profilu – wszystkie pola opcjonalne, żeby uniknąć crashy przy różnym JSON.
data class UserProfileResponse(
    val displayName: String? = null,            // top-level (jeśli backend tak zwraca)
    val username: String? = null,
    val email: String? = null,
    val description: String? = null,            // fallback jeśli backend używa 'description'
    @SerializedName("bio") val bio: String? = null, // jeśli pole ma nazwę bio
    val interests: List<String>? = null,
    val stats: UserStats? = null,
    val profile: ProfileInner? = null           // zagnieżdżony profil (częsty wzorzec)
) {
    val effectiveDisplayName: String?
        get() = displayName ?: profile?.displayName
    val effectiveDescription: String?
        get() = description ?: bio ?: profile?.bio
}

data class ProfileInner(
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
    val profile: ProfileInner? = null
)

data class UserStats(
    val friends: Int? = null,
    val posts: Int? = null,
    val likes: Int? = null
)

// Request do aktualizacji profilu – dopasowany do validatorów w userRoutes.js (profile.*)
data class UpdateProfileRequest(
    val profile: ProfilePatch
)

data class ProfilePatch(
    val displayName: String? = null,
    val gender: String? = null,
    val birthDate: String? = null, // ISO8601 jeśli użyte
    val location: String? = null,
    val bio: String? = null,
    val broadcastMessage: String? = null
)
