package com.example.projektiop.data.api

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body

// Endpoint bazuje na server.js: app.use('/api/chats', chatRoutes)
// Zakładamy GET /api/chats zwraca listę czatów bieżącego użytkownika.
interface ChatApi {
    // fetchChats controller
    @GET("chats")
    suspend fun getChats(): Response<List<ChatDto>>

    // accessChat controller: POST /chats { userId }
    @POST("chats")
    suspend fun accessChat(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ChatDto>

    // allMessages controller: GET /messages/:chatId
    @GET("messages/{chatId}")
    suspend fun getMessages(@Path("chatId") chatId: String): Response<MessagesPageDto>

    // sendMessage controller: POST /messages { chatId, content }
    @POST("messages")
    suspend fun sendMessage(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<MessageDto>
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
    // Server may return chatId either as a string or as an object with _id field.
    // Use JsonElement to handle both shapes and normalize later in mapping.
    val chatId: JsonElement? = null,
    val content: String? = null,
    val senderId: ChatUserDto? = null,
    val readBy: List<ChatUserDto>? = null,
    val createdAt: String? = null
)

data class MessagesPageDto(
    val messages: List<MessageDto>? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val totalMessages: Int? = null
)