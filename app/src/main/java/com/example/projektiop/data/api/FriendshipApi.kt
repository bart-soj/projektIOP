package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Minimal API dla listy znajomości zalogowanego użytkownika.
// Endpoint bazuje na server.js: app.use('/api/friendships', friendshipRoutes) oraz friendshipRoutes.get('/') -> GET /api/friendships
interface FriendshipApi {
    @GET("friendships")
    suspend fun getFriendships(@Query("status") status: String? = null): Response<List<FriendshipDto>>
}

data class FriendshipDto(
    val _id: String? = null,
    val userId: UserRef? = null,          // jedna strona (może być initiating user)
    val friendId: UserRef? = null,        // druga strona
    val status: String? = null,
    val friendshipType: String? = null
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