package com.example.interestapp

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BLEScanner(private val context: Context) {
    private val scanner: BluetoothLeScanner?
    private val handler = Handler()

    companion object {
        private const val CHANNEL_ID = "BLE_FOUND_DEVICE"
    }

    init {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        scanner = adapter.bluetoothLeScanner
        createNotificationChannel()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        if (scanner == null) {
            Log.e("BLEScanner", "BLE scanning not supported")
            return
        }

        handler.postDelayed({ stopScanning() }, 10000) // Automatyczne zatrzymanie po 10 sekundach
        scanner.startScan(scanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        scanner?.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: "Nieznane urządzenie"
            val deviceAddress = result.device.address
            Log.d("BLEScanner", "Znaleziono urządzenie: $deviceName ($deviceAddress)")

            // Sprawdzenie uprawnień do powiadomień na Androidzie 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w("BLEScanner", "Brak uprawnień do powiadomień, pomijam ich wysyłanie.")
                    return
                }
            }

            // Jeśli mamy uprawnienia, wysyłamy powiadomienie
            showNotification(deviceName, deviceAddress)
        }
    }


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(deviceName: String, deviceAddress: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Znaleziono urządzenie BLE!")
            .setContentText("$deviceName ($deviceAddress)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "BLE Device Found"
            val descriptionText = "Powiadomienia o znalezionych urządzeniach BLE"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
