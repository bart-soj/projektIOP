package com.example.projektiop.data.repositories

import android.content.Context
import com.example.projektiop.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.ChatListItem
import com.example.projektiop.data.repositories.UserRepository

/**
 * Periodically polls chats to detect new incoming messages and exposes a StateFlow for UI.
 * Uses unread flag from ChatRepository + lastMessageTime change to decide notifications.
 */
object ChatUpdateManager {
    private const val POLL_INTERVAL_MS = 15000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _chatsFlow = MutableStateFlow<List<ChatListItem>>(emptyList())
    val chatsFlow: StateFlow<List<ChatListItem>> = _chatsFlow

    @Volatile private var started = false
    private val lastMessageTimes: MutableMap<String, String?> = mutableMapOf()

    fun start(context: Context) {
        if (started) return
        started = true
        scope.launch {
            // Resolve current username once (used for other participant detection)
            val profile = UserRepository.fetchMyProfile().getOrNull()
            val username = profile?.username ?: profile?.effectiveDisplayName
            while (true) {
                try {
                    ChatRepository.fetchChats(currentUserId = null, currentUsername = username)
                        .onSuccess { list ->
                            detectNotifications(context, list)
                            _chatsFlow.value = list
                        }
                    // failures silently ignored this cycle
                } catch (_: Exception) { }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun detectNotifications(context: Context, newList: List<ChatListItem>) {
        newList.forEach { item ->
            val prevTime = lastMessageTimes[item.id]
            val currentTime = item.lastMessageTime
            if (prevTime != null && currentTime != null && currentTime > prevTime && item.unread) {
                // New unread message arrived
                NotificationHelper.notifyMessage(
                    context,
                    fromUser = item.title,
                    preview = item.lastMessage.take(100)
                )
            }
            if (currentTime != null) {
                lastMessageTimes[item.id] = currentTime
            }
        }
    }
}
