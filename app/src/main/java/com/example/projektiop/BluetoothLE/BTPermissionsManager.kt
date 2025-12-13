package com.example.projektiop.BluetoothLE

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.ComponentActivity
import androidx.core.location.LocationManagerCompat
import com.example.projektiop.R


class BTPermissionsManager(private val context: Context) {
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val TAG_LOCATION = "BLE_LOCATION_CHECK"
    private val TAG_PERMISSIONS = "BLE_PERMISSIONS"

    fun isLocationEnabled(): Boolean {
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun showBluetoothLocationSnackbar(context: Context) {
        var text: String = ""
        if (!isBluetoothEnabled())
            text += context.getString(R.string.enable_bluetooth) + " "

        if (!isLocationEnabled())
            text += context.getString(R.string.enable_location)

        if (text.isNotBlank())
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            // ACCESS_FINE_LOCATION jest nadal zalecane dla pełnej funkcjonalności skanowania
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else { // Poniżej Androida 12
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            // ACCESS_FINE_LOCATION jest kluczowe do skanowania BLE poniżej A12
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.toTypedArray()
    }

    fun getRequiredPermissionsScan(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // BLUETOOTH_ADMIN jest potrzebny do start/stopScan poniżej A12
            arrayOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun getRequiredPermissionsAdvertise(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            // BLUETOOTH_ADMIN jest potrzebny do start/stopAdvertising poniżej A12
            arrayOf(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    fun hasPermissions(permissions: Array<String>): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        val hasAll = permissions.all { permission ->
            val granted = ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG_PERMISSIONS, "Brakujące uprawnienie: $permission")
            }
            granted
        }
        Log.d(TAG_PERMISSIONS, "Sprawdzenie uprawnień [${permissions.joinToString()}]: $hasAll")
        return hasAll
    }

    fun requestBluetoothPermissions(activity: Activity, launcher: ActivityResultLauncher<Array<String>>) {
        val allRequiredPermissions = this.getRequiredPermissions()
        val missingPermissions = allRequiredPermissions.filter {
            ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            Log.i(TAG_PERMISSIONS, "Żądanie brakujących uprawnień: ${missingPermissions.joinToString()}")
            launcher.launch(missingPermissions)
        } else {
            Log.d(TAG_PERMISSIONS, "Wszystkie wymagane uprawnienia (${allRequiredPermissions.joinToString()}) są już przyznane.")
        }
    }

    fun showPermissionDeniedMessage(context: Context, permission: String) {
        Toast.makeText(context, "Odmówiono uprawnienia: $permission", Toast.LENGTH_SHORT).show()
    }

    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        // Sprawdź, czy jest aktywność obsługująca ten intent
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Nie można otworzyć ustawień lokalizacji.", Toast.LENGTH_SHORT).show()
        }
    }
}