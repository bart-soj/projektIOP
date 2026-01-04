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
import com.example.projektiop.R
import com.example.projektiop.util.NotificationHelper
import com.example.projektiop.util.Result
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.internal.wait
import org.koin.android.ext.android.inject
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import com.example.projektiop.domain.models.Chat as DomainChat

class ChatUpdateService(): Service() {

    private val userRepository: UserRepository by inject()
    private val chatRepository: ChatRepository by inject()

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Lista czatów
    private val _chatsFlow = MutableStateFlow<List<DomainChat>>(emptyList())
    val chatsFlow: StateFlow<List<DomainChat>> = _chatsFlow.asStateFlow()

    // User data
    private val myUserState = userRepository.myUser

    private var pollingJob: Job? = null

    private val lastMessageTimes: MutableMap<String, Instant?> = mutableMapOf()

    private var waitCounter: Int = 0
    private var waitDuration: Int = 1
    private val waitDoubling = intArrayOf(12, 18, 21, 23, 24, 25, 26, 27) // max wait time 256 * 5 = ~ 21 min

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

        scope.launch {
            startPolling()
            Log.d("ChatUpdateService", "Service Started for user: ${myUserState.value?.username}")
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
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_content))
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
                var newNotification = false
                val user = myUserState.value
                if (user != null) {
                    try {
                        val result = chatRepository.fetchChats(
                             user._id.toHexString(),
                        )

                        when (result) {
                            is Result.Error ->
                                Log.e("ChatUpdateService", "Error fetching chats")
                            is Result.Success -> {
                                val list = result.data
                                newNotification = detectNotifications(list)
                                _chatsFlow.value = list
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ChatUpdateService", "Exception in polling", e)
                    }
                }

                if (newNotification) {
                    waitCounter = 0
                    waitDuration = 1
                } else if (waitCounter != waitDoubling.last()) {
                    waitCounter++
                    if (waitCounter in waitDoubling) {
                        waitDuration *= 2
                    }
                }

                delay(POLL_INTERVAL_MS * waitDuration)
            }
        }
    }

    @SuppressLint("MissingPermission") // todo: handle permissions properly
    private fun detectNotifications(newList: List<DomainChat>): Boolean {
        var out = false
        newList.forEach { item ->
            val prevTime = lastMessageTimes[item.id]
            val currentTime = item.lastMessage?.createdAt

            // Powiadomienia
            if (prevTime != null && currentTime != null) {
                if (currentTime > prevTime && item.unread) {
                    NotificationHelper.notifyMessage(
                        this, // Context serwisu
                        fromUser = item.title,
                        preview = item.lastMessage.content.take(100)
                    )
                    out = true
                }
            }
            if (currentTime != null) {
                lastMessageTimes[item.id] = currentTime
            }
        }
        return out
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ChatUpdateService", "Service Destroyed")
        scope.cancel() // Anuluj wszystkie korutyny
    }
}
