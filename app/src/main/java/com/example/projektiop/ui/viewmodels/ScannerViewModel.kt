package com.example.projektiop.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.BluetoothLE.BLEActions
import com.example.projektiop.BluetoothLE.BluetoothRepository
import com.example.projektiop.HelloBeaconApp
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.objects.FriendshipStatus
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.OtherUserRepository
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.contains
import kotlin.collections.mapNotNull
import kotlin.collections.plus
import kotlin.math.sqrt


private const val ID: String = "_id"


data class UserWithStatus(val user: User, val status: FriendshipStatus)
data class UserWithInfo(val user: User, val status: FriendshipStatus, val friendId: String?)


class ScannerViewModel(application: Application,
                       private val userRepository: UserRepository,
                       private val friendshipRepository: FriendshipRepository,
                       private val sharedDataSource: SharedDataSource,
                       private val bleManager: BluetoothRepository) : AndroidViewModel(application) {

    // userId z SharedPreferences
    private val userId: String = sharedDataSource.get(ID, "brak")

    private val _bleEvents = MutableSharedFlow<BLEActions>()
    val bleEvents = _bleEvents.asSharedFlow<BLEActions>()

    // Publiczne StateFlow do obserwowania w UI
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val isAdvertising: StateFlow<Boolean> = bleManager.isAdvertising
    // val foundDeviceStatus: StateFlow<String> = bleManager.foundDeviceStatus
    val foundDeviceIds: StateFlow<List<String>> = bleManager.foundDeviceIds
    //private val _user = MutableStateFlow<User?>(null)
    //val user: StateFlow<User?> = _user.asStateFlow()
    val myInterests: StateFlow<List<UserInterestDto>?> = userRepository.MyUserInterests
    val friendsIds: StateFlow<List<String>> = friendshipRepository.friendsIds
    val pendingIds: StateFlow<List<String>> = friendshipRepository.pendingIds
    val blockedIds: StateFlow<List<String>> = friendshipRepository.blockedIds
    val combinedIds: StateFlow<Triple<List<String>, List<String>, List<String>>> = combine(
        friendsIds,
        pendingIds,
        blockedIds
    ) { fis, pis, bis ->
        Triple(fis, pis, bis)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Triple(emptyList(), emptyList(), emptyList())
    )

    /*
    private val _userRepositories = MutableStateFlow<List<OtherUserRepository>>(emptyList())
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _userFlows: Flow<List<Pair<User, List<UserInterestDto>?>>> = _userRepositories.flatMapLatest { repositories ->
        if (_userRepositories == emptyList<OtherUserRepository>()) {
            emptyFlow()
        } else {
            val pairs = repositories.map { repository ->
                combine(repository.UserInterests, repository.userFlow) { interests, user ->
                    assert(user != null)
                    Pair(user!!, interests)
                }
            }
            combine (pairs) { arr ->
                arr.toList()
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
     */
    /*
    val usersFromRepo: StateFlow<List<UserWithStatus>> = combine (
        myInterests,
        _userFlows,
        combinedIds
    ) { mi, uf, ids ->
        val friendsSet: Set<String> = ids.first.toSet()
        val pendingSet = ids.second.toSet()
        val blockedSet = ids.third.toSet()

        val myInterestsNames = mi?.mapNotNull{ userInterest ->
            userInterest.interest.name
        }

        val sorted = uf.sortedByDescending { pair ->
            val interests = pair.second?.mapNotNull { userInterest ->
                userInterest.interest.name
            }
            cosineSimilarity(interests?.toSet() ?: emptySet(), myInterestsNames?.toSet() ?: emptySet())
        }
        sorted.map { pair ->
            val user = pair.first
            val status: FriendshipStatus = when (user._id.toHexString()) {
                in friendsSet -> FriendshipStatus.ACCEPTED
                in pendingSet -> FriendshipStatus.PENDING
                in blockedSet -> FriendshipStatus.BLOCKED
                else -> { FriendshipStatus.NOT_FRIENDS }
            }
            UserWithStatus(user, status)
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
     */

    private val _userProfiles = MutableStateFlow<List<UserProfileResponse>>(emptyList())
    val users: StateFlow<List<UserWithStatus>> = combine(
        _userProfiles,
        myInterests,
        friendsIds,
        pendingIds,
        blockedIds
    ) {
        val friendsSet = friendsIds.value.toSet()
        val pendingSet = pendingIds.value.toSet()
        val blockedSet = blockedIds.value.toSet()

        val myInterestsNames = myInterests.value?.mapNotNull{ userInterest ->
            userInterest.interest.name
        }
        val sortedProfiles = _userProfiles.value.sortedByDescending { profile ->
            val profileInterests = profile.interests?.mapNotNull { userInterest ->
                userInterest.interest.name
            }
            cosineSimilarity(profileInterests?.toSet() ?: emptySet(), myInterestsNames?.toSet() ?: emptySet()) // sorts by this
        }
        sortedProfiles.map { profile ->
            val status: FriendshipStatus = when (profile._id) {
                in friendsSet -> FriendshipStatus.ACCEPTED
                in pendingSet -> FriendshipStatus.PENDING
                in blockedSet -> FriendshipStatus.BLOCKED
                else -> { FriendshipStatus.NOT_FRIENDS }
            }
            UserWithStatus(profile.toRealm(), status)
        }// returns this

    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList<UserWithStatus>())

    init {
        /*
        viewModelScope.launch {
            UserRepository.getMyUserFlow().collect { updatedUser ->
                _user.value = updatedUser
            }
        }
        */
        viewModelScope.launch {
            foundDeviceIds.collect { currentIds ->
                val existingProfileIds = _userProfiles.value.map { it._id }.toSet()
                val idsToFetch = currentIds.filter { it !in existingProfileIds }
                if (idsToFetch.isNotEmpty()) {
                    val newProfiles = idsToFetch.map { id ->
                        userRepository.fetchUserById(id)
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
        /*
        viewModelScope.launch {
            foundDeviceIds.collect { currentIds ->
                val existingProfileIds = _userRepositories.value.map { it.getId() }.toSet()
                val idsToFetch = currentIds.filter { it !in existingProfileIds }
                if (idsToFetch.isNotEmpty()) {
                    val newProfiles = idsToFetch.map { id ->
                        userRepository.ensureRepository(id)
                    }.mapNotNull { result -> result.getOrNull() }
                    _userRepositories.update { oldProfiles ->
                        val updatedOldProfiles = oldProfiles.filter { it.getId() in currentIds }
                        updatedOldProfiles + newProfiles
                    }
                } else {
                    _userRepositories.update { oldProfiles ->
                        oldProfiles.filter { it.getId() in currentIds }
                    }
                }
            }
        }
         */
    }

    fun addFriend(userId: String): Unit {
        viewModelScope.launch{ friendshipRepository.sendFriendRequest(userId) }
    }

    // Akcje BLE
    fun startScan() {
        viewModelScope.launch {
            _bleEvents.emit(BLEActions.START_SCAN)
        }
    }
    fun stopScan() {
        viewModelScope.launch {
            _bleEvents.emit(BLEActions.STOP_SCAN)
        }
    }
    fun startAdvertising() {
        viewModelScope.launch {
            _bleEvents.emit(BLEActions.START_ADVERTISE)
        }
    }
    fun stopAdvertising() {
        viewModelScope.launch {
            _bleEvents.emit(BLEActions.STOP_ADVERTISE)
        }
    }

    fun getUserId(): String = userId
}


fun cosineSimilarity(a: Set<String>, b: Set<String>): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0

    val intersectionSize = a.intersect(b).size
    return intersectionSize / sqrt(a.size.toDouble() * b.size.toDouble())
}