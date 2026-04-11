package com.example.projektiop.BluetoothLE

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

private val SERVICE_UUID: UUID = UUID.fromString("0000DDDD-0000-1000-8000-00805F9B34FB")

private const val TAG_SCAN = "BLE_SCAN_DEBUG"
private const val TAG_ADVERTISE = "BLE_ADVERTISE_DEBUG"


class BluetoothRepository(private val context: Context) {
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private val permissionsManager by lazy {
        BTPermissionsManager(context)
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val _foundDeviceIds = MutableStateFlow<List<String>>(emptyList())
    val foundDeviceIds: StateFlow<List<String>> = _foundDeviceIds.asStateFlow()

    private val _bleEvents = MutableSharedFlow<BLEActions>()
    val bleEvents = _bleEvents.asSharedFlow<BLEActions>()

    // --- Skanowanie ---
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                val deviceAddress = scanResult.device?.address ?: "Brak adresu"
                val record = scanResult.scanRecord ?: return@let
                val serviceUuids = record.serviceUuids
                val targetParcelUuid = ParcelUuid(SERVICE_UUID)

                if (serviceUuids?.contains(targetParcelUuid) == true) {
                    val serviceData = record.getServiceData(targetParcelUuid)
                    if (serviceData != null) {
                        try {
                            val foundUserId = bytesToUserId(serviceData)

                            if (foundUserId.isNotEmpty() && !_foundDeviceIds.value.contains(foundUserId)) {
                                val updatedList = _foundDeviceIds.value + foundUserId
                                _foundDeviceIds.value = updatedList
                            } else if (foundUserId.isNotEmpty()) {
                                // id already known
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _isScanning.value = false
            val errorText = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan Failed: Already Started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Scan Failed: App Registration Failed (Check Manifest?)"
                SCAN_FAILED_INTERNAL_ERROR -> "Scan Failed: Internal Error"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan Failed: Feature Unsupported (BLE Scan not supported?)"
                SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Scan Failed: Out of Hardware Resources"
                SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "Scan Failed: Scanning Too Frequently"
                else -> "Scan Failed: Unknown error code $errorCode"
            }
            Log.e(TAG_SCAN, errorText)
        }
    }

    // --- Rozgłaszanie ---
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertisingSet: AdvertisingSet? = null

    private val advertiseSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet?,
            txPower: Int,
            status: Int
        ) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status)
            if (status == ADVERTISE_SUCCESS) {
                this@BluetoothRepository.advertisingSet = advertisingSet
                _isAdvertising.value = true
            } else {
                _isAdvertising.value = false
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            super.onAdvertisingSetStopped(advertisingSet)
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!permissionsManager.hasPermissions(permissionsManager.getRequiredPermissionsScan())) {
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            return
        }

        if (!permissionsManager.isLocationEnabled()) {
        }

        if (_isScanning.value) {
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            return
        }

        _foundDeviceIds.value = emptyList<String>()

        // Ustawienia skanowania
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val scanSettings = ScanSettings.Builder()
            .setLegacy(true) // false for extended packets
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            // .setReportDelay(0)
            .build()

        try {
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            _isScanning.value = true
        } catch (e: SecurityException) {
            _isScanning.value = false
        } catch (e: IllegalStateException) {
            _isScanning.value = false
        } catch (e: Exception) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) {
            return
        }

        if (bluetoothLeScanner == null) {
            _isScanning.value = false
            return
        }

        try {
            bluetoothLeScanner!!.stopScan(scanCallback)
            _isScanning.value = false
            if (!_isAdvertising.value) {
            } else {
            }
        } catch (e: IllegalStateException) {
            _isScanning.value = false
        } catch (e: Exception) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(myId: String) {
        if (!permissionsManager.hasPermissions(permissionsManager.getRequiredPermissionsAdvertise())) {
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            return
        }

        if (bluetoothAdapter?.isLeExtendedAdvertisingSupported == false) {
            return
        }

        if (_isAdvertising.value) {
            return
        }

        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            return
        }

        // Ustawienia rozgłaszania
        val settings = AdvertisingSetParameters.Builder()
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .setLegacyMode(true) // false for extended packets
            .setConnectable(false)
            .build()

        val parcelUuid = ParcelUuid(SERVICE_UUID)
            val serviceData : ByteArray =  try { userIdToBytes(myId)
        } catch (e: Exception) {
            return
        }

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(parcelUuid)
            .addServiceData(parcelUuid, serviceData)
            .build()

        try {
            bluetoothLeAdvertiser?.startAdvertisingSet(settings, data, null, null, null, advertiseSetCallback)

        } catch (e: SecurityException) {
            _isAdvertising.value = false
        } catch (e: IllegalStateException) {
            _isAdvertising.value = false
        } catch (e: Exception) {
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (!_isAdvertising.value) {
            return
        }

        if (bluetoothLeAdvertiser == null) {
            _isAdvertising.value = false
            return
        }

        try {
            bluetoothLeAdvertiser?.stopAdvertisingSet(advertiseSetCallback)
            _isAdvertising.value = false
        } catch (e: IllegalStateException) {
            _isAdvertising.value = false
        } catch (e: Exception) {
            _isAdvertising.value = false
        }
    }

    suspend fun startScanEvent() {
        _bleEvents.emit(BLEActions.START_SCAN)
    }
    suspend fun stopScanEvent() {
        _bleEvents.emit(BLEActions.STOP_SCAN)
    }
    suspend fun startAdvertisingEvent() {
        _bleEvents.emit(BLEActions.START_ADVERTISE)
    }
    suspend fun stopAdvertisingEvent() {
        _bleEvents.emit(BLEActions.STOP_ADVERTISE)
    }

    fun userIdToBytes(hex: String): ByteArray {
        require(hex.length == 24) { "mongodb Id must be 24 characters" }
        return ByteArray(12) {
            hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    fun bytesToUserId(bytes: ByteArray): String {
        require(bytes.size == 12) { "mongodb Id must be 12 bytes" }

        val chars = CharArray(24)
        var i = 0

        for (b in bytes) {
            val v = b.toInt() and 0xFF
            chars[i++] = "0123456789abcdef"[v ushr 4]
            chars[i++] = "0123456789abcdef"[v and 0x0F]
        }

        return String(chars)
    }

}