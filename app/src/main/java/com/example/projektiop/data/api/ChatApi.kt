package com.example.projektiop.data.api

import com.example.projektiop.domain.models.base64
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body


interface ChatApi {
    @GET("chats")
    suspend fun getChats(): Response<List<ChatDto>>

    @POST("chats")
    suspend fun accessChat(@Body body: AccesChatRequest): Response<ChatDto>

    @GET("messages/{chatId}")
    suspend fun getMessages(@Path("chatId") chatId: String): Response<MessagesPageDto>

    @POST("messages")
    suspend fun sendMessage(@Body body: SendMessageRequest): Response<MessageDto>
}


data class AccesChatRequest(
    val userId: String
)


data class SendMessageRequest (
    val content: base64,
    val chatId: String
)


data class ChatDto(
    val _id: String? = null,
    val participants: List<String>? = null,
    val lastMessage: MessageDto? = null,
    val lastMessageTimestamp: String? = null,
    val lastResetDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)


data class MessageDto(
    val _id: String? = null,
    val chatId: String? = null,
    val content: base64? = null,
    val senderId: String? = null,
    val readBy: List<String>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)


data class MessagesPageDto(
    val messages: List<MessageDto>? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val totalMessages: Int? = null,
    val historyUnavailableReason: String?
)