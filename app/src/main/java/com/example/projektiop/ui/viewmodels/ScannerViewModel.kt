package com.example.projektiop.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.BluetoothLE.BluetoothRepository
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.realm.objects.User
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.SearchProfileRepository
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.models.FriendshipStatus
import com.example.projektiop.domain.models.Interest
import com.example.projektiop.domain.models.SearchProfile
import com.example.projektiop.util.DataError
import com.example.projektiop.util.Result
import com.example.projektiop.util.cosineSimilarity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.contains
import kotlin.collections.mapNotNull
import kotlin.collections.plus


private const val ID: String = "_id"


data class UserWithStatus(val user: User, val status: FriendshipStatus)
data class UserWithInfo(val user: User, val status: FriendshipStatus, val friendId: String?)


class ScannerViewModel(application: Application,
                       private val userRepository: UserRepository,
                       private val friendshipRepository: FriendshipRepository,
                       private val bleManager: BluetoothRepository,
                       private val searchProfileRepository: SearchProfileRepository,
                       private val interestRepository: InterestRepository) : AndroidViewModel(application) {

    // Publiczne StateFlow do obserwowania w UI
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val isAdvertising: StateFlow<Boolean> = bleManager.isAdvertising
    // val foundDeviceStatus: StateFlow<String> = bleManager.foundDeviceStatus
    val foundDeviceIds: StateFlow<List<String>> = bleManager.foundDeviceIds
    //private val _user = MutableStateFlow<User?>(null)
    //val user: StateFlow<User?> = _user.asStateFlow()
    val publicInterests = interestRepository.publicInterests
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

    private val _searchSearchProfileText = MutableStateFlow("")
    val searchSearchProfileText: StateFlow<String> = _searchSearchProfileText.asStateFlow()

    private val _searchProfileList = MutableStateFlow<List<SearchProfile>>(emptyList())
    val searchProfileList = combine(_searchProfileList, searchSearchProfileText) { list, searchText ->
        if (searchText.isNotBlank()) {
            list.filter {
                it.name.contains(searchText, ignoreCase = true)
            }
        } else {
           list
        }
    }.stateIn (
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchProfileError = MutableStateFlow<String?>(null)
    val searchProfileError = _searchProfileError.asStateFlow()

    private val _searchProfileLoading = MutableStateFlow<Boolean>(false)
    val searchProfileLoading = _searchProfileLoading.asStateFlow()

    private val _defaultSearchProfile = MutableStateFlow<SearchProfile>(SearchProfile(name = "Default", interests = emptyList()))
    private val _chosenSearchProfile = MutableStateFlow<SearchProfile?>(null)
    val searchProfile: StateFlow<SearchProfile> = combine (
        _defaultSearchProfile,
        _chosenSearchProfile
    ) { default, chosen ->
        chosen ?: default
    }.stateIn(viewModelScope,started = SharingStarted.WhileSubscribed(5000), initialValue = _defaultSearchProfile.value)

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
        searchProfile,
        friendsIds,
        pendingIds,
        blockedIds
    ) {
        val friendsSet = friendsIds.value.toSet()
        val pendingSet = pendingIds.value.toSet()
        val blockedSet = blockedIds.value.toSet()

        val myInterestsNames = searchProfile.value.interests.map { interest ->
            interest.name
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
        viewModelScope.launch {
            myInterests.collect {
                _defaultSearchProfile.value = searchProfileRepository.getDefaultSearchProfile()
            }
        }
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

    //--------------
    // BLEActions
    //--------------
    fun startScan() {
        viewModelScope.launch {
            bleManager.startScanEvent()
        }
    }
    fun stopScan() {
        viewModelScope.launch {
            bleManager.stopScanEvent()
        }
    }
    fun startAdvertising() {
        viewModelScope.launch {
            bleManager.startAdvertisingEvent()
        }
    }
    fun stopAdvertising() {
        viewModelScope.launch {
            bleManager.stopAdvertisingEvent()
        }
    }

    //--------------
    // Search Profile Stuff
    //--------------
    fun chooseSearchProfile(searchProfile: SearchProfile) {
        _chosenSearchProfile.value = searchProfile
    }

    fun addSearchProfile(name: String, interests: Set<String>) {
        _searchProfileLoading.value = true
        viewModelScope.launch {
            val resolvedInterests = interests.mapNotNull { interestRepository.getInterestByName(it) }
            val toAdd = SearchProfile(name, resolvedInterests)
            val result = searchProfileRepository.addSearchProfile(toAdd)
            when (result) {
                is Result.Error -> when(result.error) {
                    DataError.Local.DISK_FULL -> _searchProfileError.value = "no disk space"
                    DataError.Local.DB_ERROR -> _searchProfileError.value = "db error ${result.error}"
                    DataError.Local.NO_DATA -> _searchProfileError.value = "No local data"
                }
                is Result.Success ->
                    refreshSearchProfileList()
            }
            _searchProfileLoading.value = false
        }
    }

    fun refreshSearchProfileList() {
        _searchProfileLoading.value = true
        viewModelScope.launch {
            _searchProfileError.value = null
            val result = searchProfileRepository.getSearchProfiles()
            when (result) {
                is Result.Error -> when(result.error) {
                    DataError.Local.DISK_FULL -> _searchProfileError.value = "no disk space"
                    DataError.Local.DB_ERROR -> _searchProfileError.value = "db error"
                    DataError.Local.NO_DATA -> _searchProfileError.value = "No local data"
                }
                is Result.Success ->
                    _searchProfileList.value = result.data
            }
            _searchProfileLoading.value = false
        }
    }

    fun deleteSearchProfile(searchProfile: SearchProfile) {
        _searchProfileLoading.value = true
        viewModelScope.launch {
            if (_chosenSearchProfile.value?.name == searchProfile.name)
                _chosenSearchProfile.value = null
            val result = searchProfileRepository.deleteSearchProfile(searchProfile)
            when (result) {
                is Result.Error -> when(result.error) {
                    DataError.Local.DISK_FULL -> _searchProfileError.value = "no disk space"
                    DataError.Local.DB_ERROR -> _searchProfileError.value = "db error"
                    DataError.Local.NO_DATA -> _searchProfileError.value = "No local data"
                }
                is Result.Success ->
                    refreshSearchProfileList()
            }
            _searchProfileLoading.value = false
        }
    }

    fun searchForSearchProfile(string: String) {
        _searchSearchProfileText.value = string
    }
}