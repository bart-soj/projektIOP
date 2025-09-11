package com.example.projektiop.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.projektiop.R

object NotificationHelper {
    const val CHANNEL_FRIEND = "friend_events"
    const val CHANNEL_MESSAGES = "chat_messages"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val friend = NotificationChannel(CHANNEL_FRIEND, "Zaproszenia", NotificationManager.IMPORTANCE_DEFAULT)
            val messages = NotificationChannel(CHANNEL_MESSAGES, "Wiadomości", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(friend)
            nm.createNotificationChannel(messages)
        }
    }

    fun notifyFriendRequest(context: Context, fromUser: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_FRIEND)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nowe zaproszenie do znajomych")
            .setContentText(fromUser)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notif)
    }

    fun notifyMessage(context: Context, fromUser: String, preview: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nowa wiadomość od $fromUser")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notif)
    }
}
