package com.example.projektiop.BluetoothLE

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.projektiop.HelloBeaconApp
import com.example.projektiop.R
import com.example.projektiop.data.SharedDataSource
import com.example.projektiop.util.NotificationHelper.CHANNEL_BLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject


enum class BLEActions {
    STOP, START_SCAN, STOP_SCAN, START_ADVERTISE, STOP_ADVERTISE
}


class BLEService : Service() {
    private val sharedDataSource by inject<SharedDataSource>()
    private val bleManager: BluetoothRepository by inject()
    private lateinit var userId: String
    private val notificationId: Int = 1
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            BLEActions.STOP.toString() -> { stop(); return START_NOT_STICKY }
            BLEActions.START_ADVERTISE.toString() -> startAdvertise()
            BLEActions.START_SCAN.toString() -> startScan()
            BLEActions.STOP_SCAN.toString() -> stopScan()
            BLEActions.STOP_ADVERTISE.toString() -> stopAdvertise()
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        userId = sharedDataSource.get("_id", "")
        startForeground(notificationId, createNotification(getString(R.string.ble_status_inactive)))

        serviceScope.launch {
            combine ( bleManager.isScanning, bleManager.isAdvertising ) { isScanning, isAdvertising ->
                val text = when {
                    isScanning && isAdvertising -> getString(R.string.ble_status_scanning_and_advertising)
                    isScanning -> getString(R.string.ble_status_scanning)
                    isAdvertising -> getString(R.string.ble_status_advertising)
                    else -> getString(R.string.ble_status_inactive)
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(notificationId, createNotification(text))
            }.collect()
        }
    }

    private fun stop() {
        bleManager.stopScan()
        bleManager.stopAdvertising()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startScan() {
        bleManager.startScan()
    }

    private fun startAdvertise() {
        assert(userId.isNotBlank())
        bleManager.startAdvertising(userId)
    }

    private fun stopScan() {
        bleManager.stopScan()
    }

    private fun stopAdvertise() {
        bleManager.stopAdvertising()
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_BLE)
            .setContentTitle("HelloBeacon")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setDeleteIntent(PendingIntent.getService(this, 0, Intent(this, BLEService::class.java).apply {action=BLEActions.STOP.toString()}, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }
}