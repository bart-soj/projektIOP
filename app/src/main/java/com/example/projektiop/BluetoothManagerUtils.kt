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
import java.nio.ByteBuffer
import java.util.*

class BluetoothManagerUtils(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter.bluetoothLeAdvertiser

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val scanRecord = result?.scanRecord
            val serviceDataMap = scanRecord?.serviceData
            val serviceData = serviceDataMap?.get(ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")))

            val number = serviceData?.let {
                if (it.size >= 4) ByteBuffer.wrap(it).int else null
            }

            try {
                result?.device?.let { device ->
                    val name = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        device.name ?: "Brak nazwy"
                    } else {
                        "Brak uprawnień"
                    }

                    val address = device.address
                    val numberInfo = number?.toString() ?: "Brak"
                    Log.d("BLE_SCAN", "Znaleziono urządzenie: $name - $address - Liczba: $numberInfo")
                    showToast("Znaleziono: $name - $address\nLiczba: $numberInfo")
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
        showToast("Rozpoczęto skanowanie")

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
        showToast("Zatrzymano skanowanie")

        if (!checkPermissions()) return

        try {
            scanner?.stopScan(scanCallback)
            Log.d("BLE_SCAN", "Zatrzymano skanowanie BLE")
        } catch (e: SecurityException) {
            Log.e("BLE_SCAN", "SecurityException: ${e.message}")
        }
    }

    fun startAdvertising() {
        showToast("Rozpoczęto rozgłaszanie")

        if (!checkPermissions()) return

        val randomNumber = (0..9999).random() // Losowa liczba
        val serviceUuid = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
        val serviceData = ByteBuffer.allocate(4).putInt(randomNumber).array()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceUuid))
            .addServiceData(ParcelUuid(serviceUuid), serviceData)
            .build()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            Log.d("BLE_ADVERTISE", "Rozpoczęto rozgłaszanie BLE z liczbą: $randomNumber")
        } catch (e: SecurityException) {
            Log.e("BLE_ADVERTISE", "SecurityException: ${e.message}")
        }
    }

    fun stopAdvertising() {
        showToast("Zatrzymano rozgłaszanie")

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
