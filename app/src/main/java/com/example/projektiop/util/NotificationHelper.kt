package com.example.projektiop.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.projektiop.R

object NotificationHelper {
    val CHANNEL_FRIEND = "friend_events"
    val CHANNEL_MESSAGES = "chat_messages"
    val CHANNEL_BLE = "ble_service"

    fun initChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val friend = NotificationChannel(CHANNEL_FRIEND, context.getString(R.string.channel_friend_name), NotificationManager.IMPORTANCE_DEFAULT)
        val bleservice = NotificationChannel(CHANNEL_BLE, context.getString(R.string.channel_ble_name), NotificationManager.IMPORTANCE_DEFAULT)
        val messages = NotificationChannel(CHANNEL_MESSAGES, context.getString(R.string.channel_messages_name), NotificationManager.IMPORTANCE_HIGH)
        nm.createNotificationChannel(friend)
        nm.createNotificationChannel(messages)
        nm.createNotificationChannel(bleservice)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyFriendRequest(context: Context, fromUser: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_FRIEND)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.new_friend_request))
            .setContentText(fromUser)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notif)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyMessage(context: Context, fromUser: String, preview: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_new_message, fromUser))
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notif)
    }
}
