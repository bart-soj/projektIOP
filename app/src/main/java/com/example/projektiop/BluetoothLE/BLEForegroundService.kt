package com.example.interestapp

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat

class BLEForegroundService : Service() {
    private lateinit var scanner: BluetoothLeScanner

    companion object {
        const val CHANNEL_ID = "BLE_SERVICE_CHANNEL"
        const val ACTION_START = "START_BLE"
        const val ACTION_STOP = "STOP_BLE"
        const val ACTION_STATUS_UPDATE = "BLE_STATUS_UPDATE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val adapter = BluetoothAdapter.getDefaultAdapter()
        scanner = adapter.bluetoothLeScanner
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startScanning()
            }
            ACTION_STOP -> {
                stopScanning()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BLEForegroundService", "Brak uprawnień do Foreground Service!")
                return
            }
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE w tle")
            .setContentText("Skanowanie BLE aktywne...")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
        sendStatusUpdate("Skanowanie BLE aktywne")
        Log.d("BLEForegroundService", "Foreground Service uruchomiony poprawnie")
    }

    private fun sendNotification(title: String, message: String, notificationId: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }

    private fun startScanning() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            scanner.startScan(scanCallback)
            sendNotification("BLE Skanowanie", "Rozpoczęto skanowanie urządzeń BLE.", 100)
            sendStatusUpdate("Skanowanie BLE w toku")
            Log.d("BLEForegroundService", "Rozpoczęto skanowanie BLE")

            Handler(mainLooper).postDelayed({
                stopScanning()
                Log.d("BLEForegroundService", "Skanowanie BLE zatrzymane automatycznie")
            }, 30000)
        } else {
            Log.e("BLEForegroundService", "Brak uprawnień do skanowania BLE!")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopScanning() {
        scanner.stopScan(scanCallback)
        sendNotification("BLE Skanowanie", "Zatrzymano skanowanie urządzeń BLE.", 101)
        sendStatusUpdate("Skanowanie BLE zatrzymane")
        Log.d("BLEForegroundService", "Zatrzymano skanowanie BLE")
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: "Nieznane urządzenie"
            val deviceAddress = result.device.address

            sendNotification("Znaleziono urządzenie BLE", "$deviceName ($deviceAddress)", 102)
            sendStatusUpdate("Znaleziono: $deviceName ($deviceAddress)")
            Log.d("BLEForegroundService", "Znaleziono urządzenie: $deviceName ($deviceAddress)")
        }
    }

    private fun sendStatusUpdate(status: String) {
        val intent = Intent(ACTION_STATUS_UPDATE)
        intent.putExtra("status", status)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BLE Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
