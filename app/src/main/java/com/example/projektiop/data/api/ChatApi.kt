package com.example.projektiop.data.api

import retrofit2.Response
import retrofit2.http.GET

// Endpoint bazuje na server.js: app.use('/api/chats', chatRoutes)
// Zakładamy GET /api/chats zwraca listę czatów bieżącego użytkownika.
interface ChatApi {
    @GET("chats")
    suspend fun getChats(): Response<List<ChatDto>>
}

data class ChatDto(
    val _id: String? = null,
    val participants: List<ChatUserDto>? = null,
    val lastMessage: MessageDto? = null
)

data class ChatUserDto(
    val _id: String? = null,
    val username: String? = null,
    val profile: ProfileRef? = null
)

data class MessageDto(
    val _id: String? = null,
    val content: String? = null,
    val senderId: ChatUserDto? = null,
    val createdAt: String? = null
)