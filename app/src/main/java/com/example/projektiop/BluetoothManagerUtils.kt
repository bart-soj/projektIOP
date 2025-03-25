import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.util.*

class BluetoothManagerUtils(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter.bluetoothLeAdvertiser

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            try {
                result?.device?.let { device ->
                    val name = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        device.name ?: "Brak nazwy"
                    } else {
                        "Brak uprawnień"
                    }

                    val address = device.address // address nie wymaga CONNECT
                    Log.d("BLE_SCAN", "Znaleziono urządzenie: $name - $address")
                }
            } catch (e: SecurityException) {
                Log.e("BLE_SCAN", "Brak uprawnień do odczytu nazwy urządzenia: ${e.message}")
            }
        }
    }


    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d("BLE_ADVERTISE", "Rozpoczęto rozgłaszanie BLE")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE_ADVERTISE", "Błąd rozgłaszania BLE: $errorCode")
        }
    }

    fun startScan() {
        showToast("Start Scan kliknięty")

        if (!checkPermissions()) return

        try {
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner?.startScan(null, scanSettings, scanCallback)
            Log.d("BLE_SCAN", "Rozpoczęto skanowanie BLE")
        } catch (e: SecurityException) {
            Log.e("BLE_SCAN", "SecurityException: ${e.message}")
        }
    }

    fun stopScan() {
        showToast("Stop Scan kliknięty")

        if (!checkPermissions()) return

        try {
            scanner?.stopScan(scanCallback)
            Log.d("BLE_SCAN", "Zatrzymano skanowanie BLE")
        } catch (e: SecurityException) {
            Log.e("BLE_SCAN", "SecurityException: ${e.message}")
        }
    }

    fun startAdvertising() {
        showToast("Start Advertise kliknięty")

        if (!checkPermissions()) return

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")))
                .build()

            advertiser?.startAdvertising(settings, data, advertiseCallback)
            Log.d("BLE_ADVERTISE", "Rozpoczęto rozgłaszanie BLE")
        } catch (e: SecurityException) {
            Log.e("BLE_ADVERTISE", "SecurityException: ${e.message}")
        }
    }

    fun stopAdvertising() {
        showToast("Stop Advertise kliknięty")

        if (!checkPermissions()) return

        try {
            advertiser?.stopAdvertising(advertiseCallback)
            Log.d("BLE_ADVERTISE", "Zatrzymano rozgłaszanie BLE")
        } catch (e: SecurityException) {
            Log.e("BLE_ADVERTISE", "SecurityException: ${e.message}")
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = permissions.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (missing.isEmpty()) {
            true
        } else {
            showToast("Brakuje uprawnień: ${missing.joinToString()}")
            false
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
