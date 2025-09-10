package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import com.google.gson.annotations.SerializedName

// Minimal API dla listy znajomości zalogowanego użytkownika.
// Endpoint bazuje na server.js: app.use('/api/friendships', friendshipRoutes) oraz friendshipRoutes.get('/') -> GET /api/friendships
interface FriendshipApi {
    @GET("friendships")
    suspend fun getFriendships(
        @Query("status") status: String? = null,
        @Query("direction") direction: String? = null
    ): Response<List<FriendshipDto>>

    @POST("friendships/request")
    suspend fun sendRequest(@Body body: FriendRequest): Response<Unit>

    @retrofit2.http.PUT("friendships/{id}/accept")
    suspend fun acceptRequest(@retrofit2.http.Path("id") friendshipId: String): Response<Unit>

    @retrofit2.http.PUT("friendships/{id}/reject")
    suspend fun rejectRequest(@retrofit2.http.Path("id") friendshipId: String): Response<Unit>
}

data class FriendRequest(
    @SerializedName("friendId") val friendId: String? = null,
    @SerializedName("recipientId") val recipientId: String? = null
)

// Backend (kontroler getFriendships) zwraca już PRZETWORZONE obiekty:
// { friendshipId, user: { _id, username, profile { displayName, avatarUrl } }, status, friendshipType, isPendingRecipient, ... }
// Dodajemy też pola userId/friendId dla kompatybilności jeśli kiedyś backend zwróci surowe dane.
data class FriendshipDto(
    val friendshipId: String? = null,
    val user: UserRef? = null,
    val status: String? = null,
    val friendshipType: String? = null,
    val isPendingRecipient: Boolean? = null,
    val requestedByUsername: String? = null,
    val isBlocked: Boolean? = null,
    val blockedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    // Legacy / fallback
    val _id: String? = null,
    val userId: UserRef? = null,
    val friendId: UserRef? = null
)

data class UserRef(
    val _id: String? = null,
    val username: String? = null,
    val profile: ProfileRef? = null
)

data class ProfileRef(
    val displayName: String? = null,
    val avatarUrl: String? = null
)