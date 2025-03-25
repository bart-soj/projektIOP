package com.example.projektiop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

fun requestBluetoothPermissions(
    activity: android.app.Activity,
    launcher: ActivityResultLauncher<Array<String>>
) {
    // Deklaracja typu: MutableList<String>
    val permissions: MutableList<String> = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)

    // Android 12+ (API 31+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
    }

    // Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    // Filtrowanie uprawnień, które jeszcze nie zostały przyznane
    val permissionsToRequest = permissions.filter {
        ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
    }

    // Jeśli są uprawnienia do zażądania, to je prosimy
    if (permissionsToRequest.isNotEmpty()) {
        Log.d("PermissionRequest", "Requesting permissions: $permissionsToRequest")
        launcher.launch(permissionsToRequest.toTypedArray())
    } else {
        Log.d("PermissionRequest", "All permissions already granted.")
    }
}


fun showPermissionDeniedMessage(permission: String) {
    when (permission) {
        Manifest.permission.ACCESS_FINE_LOCATION -> {
            // Możesz wyświetlić np. Toast lub AlertDialog
            Log.d("PermissionRequest", "Dostęp do lokalizacji został odrzucony!")
        }
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT -> {
            Log.d("PermissionRequest", "Uprawnienia Bluetooth zostały odrzucone!")
        }
        Manifest.permission.NEARBY_WIFI_DEVICES -> {
            Log.d("PermissionRequest", "Uprawnienia do urządzeń w pobliżu zostały odrzucone!")
        }
    }
}