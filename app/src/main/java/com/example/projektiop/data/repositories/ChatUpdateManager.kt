package com.example.projektiop.data.repositories

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.collectAsState
import com.example.projektiop.HelloBeaconApp
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ChatUpdateManager { // TODO() start it at appropriate place, bugged now, make into a service
    private const val POLL_INTERVAL_MS = 15000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _chatsFlow = MutableStateFlow<List<ChatListItem>>(emptyList())
    val chatsFlow: StateFlow<List<ChatListItem>> = _chatsFlow
    val MyUserStateFlow = MutableStateFlow<User?>(null)

    @Volatile private var started = false
    private val lastMessageTimes: MutableMap<String, String?> = mutableMapOf()

    fun start(context: Context) {
        if (started) return
        started = true
        scope.launch {
            UserRepository.myUserFlow.collect { value -> MyUserStateFlow.value = value }
        }

        if (MyUserStateFlow.value != null ) {
            scope.launch {
                while (started) {
                    try {
                        ChatRepository.fetchChats(
                            currentUserId = MyUserStateFlow.value?._id?.toHexString(),
                            currentUsername = MyUserStateFlow.value?.username
                        )
                            .onSuccess { list ->
                                detectNotifications(context, list)
                                _chatsFlow.value = list
                            }
                    } catch (e: Exception) {
                        Log.d("CU", "chat update failed", e)
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    private fun detectNotifications(context: Context, newList: List<ChatListItem>) {
        newList.forEach { item ->
            val prevTime = lastMessageTimes[item.id]
            val currentTime = item.lastMessageTime
            if (prevTime != null && currentTime != null && currentTime > prevTime && item.unread) {
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


class ChatUpdateService(): Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    // TODO() implement, make ChatsViewModel/mainActivity bind to it
}
