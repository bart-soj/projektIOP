package com.example.projektiop.BluetoothLE

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import kotlinx.coroutines.flow.StateFlow

private const val ID: String = "_id"


class BLEViewModel(application: Application) : AndroidViewModel(application) {

    // userId z SharedPreferences
    private val userId: String = SharedPreferencesRepository.get(ID, "brak")

    // Instancja BLE managera z kontekstem aplikacji, aby uniknąć wycieków pamięci
    private val bleManager = BluetoothManagerUtils(application.applicationContext, userId)

    // Publiczne StateFlow do obserwowania w UI
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val isAdvertising: StateFlow<Boolean> = bleManager.isAdvertising
    val foundDeviceStatus: StateFlow<String> = bleManager.foundDeviceStatus
    val foundDeviceIds: StateFlow<List<String>> = bleManager.foundDeviceIds

    // Akcje BLE
    fun startScan() = bleManager.startScan()
    fun stopScan() = bleManager.stopScan()
    fun startAdvertising() = bleManager.startAdvertising()
    fun stopAdvertising() = bleManager.stopAdvertising()

    fun getUserId(): String = userId
}
