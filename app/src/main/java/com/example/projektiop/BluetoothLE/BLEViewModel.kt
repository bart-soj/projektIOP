package com.example.projektiop.BluetoothLE

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.objects.UserProfile
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.screens.cosineSimilarity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.mapNotNull
import kotlin.collections.plus
import kotlin.jvm.optionals.getOrNull

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
    val myProfile: StateFlow<UserProfileResponse?> = UserRepository.MyProfile

    private val _userProfiles = MutableStateFlow<List<UserProfileResponse>>(emptyList())
    val userProfiles: StateFlow<List<UserProfileResponse>> = combine(
        _userProfiles.asStateFlow(),
        myProfile
    ) {
        val myInterests = myProfile.value?.interests?.map{ userInterest ->
            userInterest.interest.name
        }
        _userProfiles.value.sortedByDescending { profile->
            val profileInterests = profile.interests?.map { userInterest ->
                userInterest.interest.name
            }
            cosineSimilarity(profileInterests?.toSet() ?: emptySet(), myInterests?.toSet() ?: emptySet())
        }

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
    fun startScan() = bleManager.startScan()
    fun stopScan() = bleManager.stopScan()
    fun startAdvertising() = bleManager.startAdvertising()
    fun stopAdvertising() = bleManager.stopAdvertising()

    fun getUserId(): String = userId
}


fun cosineSimilarity(a: Set<String>, b: Set<String>): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0

    val intersectionSize = a.intersect(b).size
    return intersectionSize / kotlin.math.sqrt(a.size.toDouble() * b.size.toDouble())
}