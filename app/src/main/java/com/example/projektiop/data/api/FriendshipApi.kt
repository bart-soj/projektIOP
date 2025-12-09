package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
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

    @PUT("friendships/{id}/accept")
    suspend fun acceptRequest(@Path("id") friendshipId: String): Response<Unit>

    @PUT("friendships/{id}/reject")
    suspend fun rejectRequest(@Path("id") friendshipId: String): Response<Unit>

    @DELETE("friendships/{id}")
    suspend fun removeFriendship(@Path("id") friendshipId: String): Response<Unit>

    @PUT("friendships/{id}/block")
    suspend fun blockFriendship(@Path("id") friendshipId: String): Response<BlockDto>

    @PUT("friendships/{id}/unblock")
    suspend fun unblockFriendship(@Path("id") friendshipId: String): Response<BlockDto>
}

data class FriendRequest(
    @SerializedName("recipientId") val recipientId: String? = null
)

data class FriendshipDto(
    val friendshipId: String? = null,
    val user: UserProfileResponse? = null,
    val status: String? = null,
    val friendshipType: String? = null,
    val isPendingRecipient: Boolean? = null,
    val requestedBy: String? = null,   // _id of the requesting user
    val isBlocked: Boolean? = null,
    val blockedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val _id = friendshipId // quickfix as it was used this way in many places
}

data class BlockDto (
    val message: String? = null,
    val friendship: FriendshipDto? = null
)
