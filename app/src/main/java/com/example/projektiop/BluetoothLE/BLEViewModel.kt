package com.example.projektiop.BluetoothLE

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.HelloBeaconApp
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.screens.cosineSimilarity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.mapNotNull
import kotlin.collections.plus


private const val ID: String = "_id"


class BLEViewModel(application: Application) : AndroidViewModel(application) {

    // userId z SharedPreferences
    private val userId: String = SharedPreferencesRepository.get(ID, "brak")

    // Instancja BLE managera z kontekstem aplikacji, aby uniknąć wycieków pamięci
    private val bleManager: BluetoothRepository = (application as HelloBeaconApp).bluetoothRepository

    private val _uiEvents = MutableSharedFlow<Actions>()
    val uiEvents = _uiEvents.asSharedFlow<Actions>()

    // Publiczne StateFlow do obserwowania w UI
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val isAdvertising: StateFlow<Boolean> = bleManager.isAdvertising
    val foundDeviceStatus: StateFlow<String> = bleManager.foundDeviceStatus
    val foundDeviceIds: StateFlow<List<String>> = bleManager.foundDeviceIds
    val myProfile: StateFlow<UserProfileResponse?> = UserRepository.MyProfile

    private val _userProfiles = MutableStateFlow<List<UserProfileResponse>>(emptyList())
    val userProfiles: StateFlow<List<UserProfileResponse>> = combine(
        _userProfiles.asStateFlow(),
        myProfile
    ) {
        val myInterests = myProfile.value?.interests?.map{ userInterest ->
            userInterest.interest.name
        }
        _userProfiles.value.sortedByDescending { profile ->
            val profileInterests = profile.interests?.map { userInterest ->
                userInterest.interest.name
            }
            cosineSimilarity(profileInterests?.toSet() ?: emptySet(), myInterests?.toSet() ?: emptySet()) // sorts by this
        } // returns this

    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList<UserProfileResponse>())

    init {
        viewModelScope.launch {
            foundDeviceIds.collect { currentIds ->
                val existingProfileIds = _userProfiles.value.map { it._id }.toSet()
                val idsToFetch = currentIds.filter { it !in existingProfileIds }
                if (idsToFetch.isNotEmpty()) {
                    val newProfiles = idsToFetch.map { id ->
                        UserRepository.fetchUserById(id)
                    }.mapNotNull { result -> result.getOrNull() }
                    _userProfiles.update { oldProfiles ->
                        val updatedOldProfiles = oldProfiles.filter { it._id in currentIds }
                        updatedOldProfiles + newProfiles
                    }
                } else {
                    _userProfiles.update { oldProfiles ->
                        oldProfiles.filter { it._id in currentIds }
                    }
                }
            }
        }
    }

    // Akcje BLE
    fun startScan() {
        viewModelScope.launch {
            _uiEvents.emit(Actions.START_SCAN)
        }
    }
    fun stopScan() {
        viewModelScope.launch {
            _uiEvents.emit(Actions.STOP_SCAN)
        }
    }
    fun startAdvertising() {
        viewModelScope.launch {
            _uiEvents.emit(Actions.START_ADVERTISE)
        }
    }
    fun stopAdvertising() {
        viewModelScope.launch {
            _uiEvents.emit(Actions.STOP_ADVERTISE)
        }
    }

    fun getUserId(): String = userId

    enum class Actions {
       STOP, START_SCAN, STOP_SCAN, START_ADVERTISE, STOP_ADVERTISE
    }
}


fun cosineSimilarity(a: Set<String>, b: Set<String>): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0

    val intersectionSize = a.intersect(b).size
    return intersectionSize / kotlin.math.sqrt(a.size.toDouble() * b.size.toDouble())
}