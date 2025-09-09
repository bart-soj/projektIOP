package com.example.projektiop.data.repositories

import com.example.projektiop.data.api.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatListItem(
    val id: String,
    val title: String,
    val lastMessage: String,
    val lastMessageTime: String? = null
)

object ChatRepository {
    suspend fun fetchChats(currentUserId: String? = null): Result<List<ChatListItem>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.chatApi.getChats()
            if (response.isSuccessful) {
                val body = response.body().orEmpty()
                val mapped = body.mapNotNull { chat ->
                    val id = chat._id ?: return@mapNotNull null
                    val participants = chat.participants.orEmpty()
                    val other = participants.firstOrNull { it._id != null && it._id != currentUserId }
                    val title = other?.profile?.displayName ?: other?.username ?: "Czat"
                    val lastMsg = chat.lastMessage?.content ?: "(brak wiadomości)"
                    ChatListItem(id = id, title = title, lastMessage = lastMsg, lastMessageTime = chat.lastMessage?.createdAt)
                }
                Result.success(mapped)
            } else {
                Result.failure(Exception("Nie udało się pobrać czatów (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}