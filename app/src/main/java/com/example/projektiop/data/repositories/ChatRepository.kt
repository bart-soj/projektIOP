package com.example.projektiop.data

import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.MessageDto
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

    suspend fun ensureChatWithUser(friendId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Na razie: próbuj znaleźć istniejący czat przez listę czatów
            val existing = RetrofitInstance.chatApi.getChats()
            if (existing.isSuccessful) {
                existing.body().orEmpty().firstOrNull { chat ->
                    chat.participants?.any { it._id == friendId } == true
                }?.let { return@withContext Result.success(it._id ?: "") }
            }
            // Prosta forma required przez accessChat: { userId }
            val created = RetrofitInstance.chatApi.accessChat(mapOf("userId" to friendId))
            if (created.isSuccessful) {
                Result.success(created.body()?._id ?: return@withContext Result.failure(Exception("Brak ID czatu")))
            } else Result.failure(Exception("Tworzenie czatu nie powiodło się (${created.code()}) ${created.errorBody()?.string()?.take(150)}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun loadMessages(chatId: String): Result<List<MessageDto>> = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitInstance.chatApi.getMessages(chatId)
            if (r.isSuccessful) Result.success(r.body()?.messages.orEmpty()) else Result.failure(Exception("Błąd pobierania wiadomości (${r.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendMessage(chatId: String, content: String): Result<MessageDto> = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitInstance.chatApi.sendMessage(mapOf("chatId" to chatId, "content" to content))
            if (r.isSuccessful) Result.success(r.body()!!) else Result.failure(Exception("Błąd wysyłania (${r.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }
}

private fun errorResult(code: Int, raw: String?): Result<String> {
    val msg = buildString {
        append("HTTP $code")
        if (!raw.isNullOrBlank()) append(": ").append(raw.take(200))
    }
    return Result.failure(Exception(msg))
}