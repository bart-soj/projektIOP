package com.example.projektiop.BluetoothLE

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
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

    private val TAG_PERMISSIONS = "BLE_PERMISSIONS"

    fun isLocationEnabled(): Boolean {
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun showBluetoothLocationSnackbar(context: Context) {
        var text = ""
        if (!isBluetoothEnabled())
            text += context.getString(R.string.enable_bluetooth) // + " "

        /*
        if (!isLocationEnabled())
            text += context.getString(R.string.enable_location)
         */

        if (text.isNotBlank())
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        // permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        return permissions.toTypedArray()
    }

    fun getRequiredPermissionsScan(): Array<String> {
        return arrayOf(Manifest.permission.BLUETOOTH_SCAN/*, Manifest.permission.ACCESS_FINE_LOCATION*/)
    }

    fun getRequiredPermissionsAdvertise(): Array<String> {
        return arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE)
    }

    fun hasPermissions(permissions: Array<String>): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        val hasAll = permissions.all { permission ->
            val granted = ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            if(!granted) { Log.d(TAG_PERMISSIONS, "missing $permission") }
            granted
        }
        return hasAll
    }

    fun requestBluetoothPermissions(activity: Activity, launcher: ActivityResultLauncher<Array<String>>) {
        val allRequiredPermissions = this.getRequiredPermissions()
        val missingPermissions = allRequiredPermissions.filter {
            ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            launcher.launch(missingPermissions)
        }
    }
}