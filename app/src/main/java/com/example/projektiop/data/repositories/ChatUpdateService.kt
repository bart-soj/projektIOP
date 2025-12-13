package com.example.projektiop.data.repositories

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.util.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChatUpdateService(): Service(), KoinComponent {

    private val userRepository: UserRepository by inject()

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Lista czatów
    private val _chatsFlow = MutableStateFlow<List<ChatListItem>>(emptyList())
    val chatsFlow: StateFlow<List<ChatListItem>> = _chatsFlow.asStateFlow()

    // User data
    private var myUserFlow: Flow<User?>? = null
    private val myUserState = MutableStateFlow<User?>(null)

    private var pollingJob: Job? = null

    private val lastMessageTimes: MutableMap<String, String?> = mutableMapOf()

    companion object {
        private const val POLL_INTERVAL_MS = 5000L
    }

    // Klasa LocalBinder zwraca instancję serwisu
    inner class LocalBinder : Binder() {
        fun getService(): ChatUpdateService = this@ChatUpdateService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ChatUpdateService", "Service Created")

        startForegroundServiceNotification()

        try {
            myUserFlow = UserRepository.myUserFlow
        } catch (e: Exception) {
            Log.e("ChatUpdateService", "Failed to get myUserFlow", e)
            stopSelf()
            return
        }


        scope.launch {
            userRepository.myUserFlow.collect { user ->
                myUserState.value = user
                startPolling()
                Log.d("ChatUpdateService", "Service Started for user: ${user?.username}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ChatUpdateService", "Service Started")
        return START_STICKY // Serwis będzie ponownie uruchamiany, jeśli zostanie zabity
    }

    private fun startForegroundServiceNotification() {
        val channelId = "chat_service_channel"
        val channelName = "Chat Service Status"
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_MIN
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("HelloBeacon Chat Service")
            .setContentText("Chat Service is working in background")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(420, notification)
    }

    private fun startPolling() {
        // Anuluj poprzednią pracę, jeśli istnieje
        pollingJob?.cancel()

        pollingJob = scope.launch {
            while (isActive) {
                val user = myUserState.value
                if (user != null) {
                    try {
                        ChatRepository.fetchChats(
                            currentUserId = user._id.toHexString(),
                            currentUsername = user.username
                        ).onSuccess { list ->
                            detectNotifications(list)
                            _chatsFlow.value = list
                        }.onFailure { e ->
                            Log.e("ChatUpdateService", "Error fetching chats", e)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatUpdateService", "Exception in polling", e)
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    @SuppressLint("MissingPermission") // todo: handle permissions properly
    private fun detectNotifications(newList: List<ChatListItem>) {
        newList.forEach { item ->
            val prevTime = lastMessageTimes[item.id]
            val currentTime = item.lastMessageTime

            // Powiadomienia
            if (prevTime != null && currentTime != null && currentTime > prevTime && item.unread) {
                NotificationHelper.notifyMessage(
                    this, // Context serwisu
                    fromUser = item.title,
                    preview = item.lastMessage.take(100)
                )
            }
            if (currentTime != null) {
                lastMessageTimes[item.id] = currentTime
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ChatUpdateService", "Service Destroyed")
        scope.cancel() // Anuluj wszystkie korutyny
    }
}


/*
object ChatUpdateManager {
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
*/