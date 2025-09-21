package com.example.projektiop.data.repositories

import com.example.projektiop.data.api.MessageDto
import com.example.projektiop.data.api.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.SharedPreferences

private const val BASE_URL_KEY: String = "BASE_URL"

// Lightweight local storage for last read timestamps per chat
private object ChatReadState {
    private const val PREFS = "chat_read_state"
    private const val KEY_PREFIX = "last_read_"
    @Volatile private var prefs: SharedPreferences? = null

    fun ensure(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun getLastRead(chatId: String): String? = prefs?.getString(KEY_PREFIX + chatId, null)

    fun markRead(chatId: String, lastMessageTime: String?) {
        if (lastMessageTime == null) return
        prefs?.edit()?.putString(KEY_PREFIX + chatId, lastMessageTime)?.apply()
    }

    fun storeBaseline(chatId: String, lastMessageTime: String) {
        // Only set if absent to avoid retroactively marking old messages unread
        if (getLastRead(chatId) == null) markRead(chatId, lastMessageTime)
    }

    fun isAfter(candidate: String, baseline: String?): Boolean {
        if (baseline == null) return false
        // ISO-8601 lexical compare works for ordering if same format
        return candidate > baseline
    }
}

// Public list item used by ChatsScreen
// Includes friendId and avatarUrl for navigation and UI

data class ChatListItem(
    val id: String,
    val title: String,
    val lastMessage: String,
    val lastMessageTime: String? = null,
    val friendId: String? = null,
    val avatarUrl: String? = null,
    val unread: Boolean = false
)

object ChatRepository {
    private fun normalizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val base = SharedPreferencesRepository.get(BASE_URL_KEY,"")
        return if (trimmed.startsWith("/")) base + trimmed else "$base/$trimmed"
    }
    // Call once at app start
    fun init(context: Context) { ChatReadState.ensure(context) }
    /** Mark a chat read by persisting last seen message timestamp */
    fun markChatRead(chatId: String, lastMessageTime: String?) {
        ChatReadState.markRead(chatId, lastMessageTime)
    }

    suspend fun fetchChats(currentUserId: String? = null, currentUsername: String? = null): Result<List<ChatListItem>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.chatApi.getChats()
            if (response.isSuccessful) {
                val body = response.body().orEmpty()
                val mapped = body.mapNotNull { chat ->
                    val id = chat._id ?: return@mapNotNull null
                    val participants = chat.participants.orEmpty()
                    val other = participants.firstOrNull { p ->
                        (currentUserId != null && p._id != null && p._id != currentUserId) ||
                        (currentUsername != null && p.username != null && p.username != currentUsername)
                    } ?: if (participants.size == 2) {
                        participants.firstOrNull { it.username != null && it.username != currentUsername } ?: participants.firstOrNull()
                    } else {
                        participants.firstOrNull()
                    }
                    val title = other?.profile?.displayName ?: other?.username ?: "Czat"
                    val lastMsg = chat.lastMessage?.content ?: "(brak wiadomości)"
                    val lastMessageTime = chat.lastMessage?.createdAt
                    val lastRead = ChatReadState.getLastRead(id)
                    // If no stored lastRead baseline, establish one to avoid flagging entire history as unread immediately
                    if (lastRead == null && lastMessageTime != null) {
                        ChatReadState.storeBaseline(id, lastMessageTime)
                    }
                    val unread = if (lastMessageTime == null) false else ChatReadState.isAfter(lastMessageTime, ChatReadState.getLastRead(id))
                    ChatListItem(
                        id = id,
                        title = title,
                        lastMessage = lastMsg,
                        lastMessageTime = lastMessageTime,
                        friendId = other?._id,
                        avatarUrl = normalizeUrl(other?.profile?.avatarUrl),
                        unread = unread
                    )
                }
                val sorted = mapped.sortedWith(
                    compareBy<ChatListItem> { it.lastMessageTime == null }
                        .thenByDescending { it.lastMessageTime }
                )
                Result.success(sorted)
            } else {
                Result.failure(Exception("Nie udało się pobrać czatów (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ensureChatWithUser(friendId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val existing = RetrofitInstance.chatApi.getChats()
            if (existing.isSuccessful) {
                existing.body().orEmpty().firstOrNull { chat ->
                    chat.participants?.any { it._id == friendId } == true
                }?.let { return@withContext Result.success(it._id ?: "") }
            }
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
