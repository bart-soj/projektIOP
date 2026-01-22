package com.example.projektiop.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.projektiop.BluetoothLE.BluetoothRepository
import com.example.projektiop.data.db.realm.objects.User
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.OtherUserRepository
import com.example.projektiop.data.repositories.SearchProfileRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.models.FriendshipStatus
import com.example.projektiop.domain.models.Interest
import com.example.projektiop.domain.models.SearchProfile
import com.example.projektiop.domain.models.UserInterest
import com.example.projektiop.domain.Result
import com.example.projektiop.domain.cosineSimilarity
import com.example.projektiop.ui.toStringRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import kotlin.collections.mapNotNull
import kotlin.collections.plus
import com.example.projektiop.domain.models.User as DomainUser


private const val ID: String = "_id"


data class UserWithStatus(val user: DomainUser, val status: FriendshipStatus)
data class UserWithInfo(val user: User, val status: FriendshipStatus, val friendId: String?)


class ScannerViewModel(application: Application,
                       private val userRepository: UserRepository,
                       private val friendshipRepository: FriendshipRepository,
                       private val bleManager: BluetoothRepository,
                       private val searchProfileRepository: SearchProfileRepository,
                       private val interestRepository: InterestRepository) : AndroidViewModel(application) {

    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val isAdvertising: StateFlow<Boolean> = bleManager.isAdvertising
    // val foundDeviceStatus: StateFlow<String> = bleManager.foundDeviceStatus
    val foundDeviceIds: StateFlow<List<String>> = bleManager.foundDeviceIds
    //private val _user = MutableStateFlow<User?>(null)
    //val user: StateFlow<User?> = _user.asStateFlow()
    val publicInterests = interestRepository.publicInterests
    val myInterests: StateFlow<List<UserInterest>?> = userRepository.MyUserInterests
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

    private val _userRepositories = MutableStateFlow<List<OtherUserRepository>>(emptyList())
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _usersWithInterests =
        _userRepositories.flatMapLatest { repoList ->
            if (repoList.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    repoList.map { repo ->
                        combine(
                            repo.Profile,
                            repo.UserInterests.map { ui ->
                                ui?.map { it.interest } ?: emptyList()
                            }
                        ) { profile, interests ->
                            profile to interests
                        }
                    }
                ) { pairs ->
                    pairs.toList()
                }
            }
        }

    val usersFromRepo: StateFlow<List<UserWithStatus>> = combine (
        searchProfile,
        _usersWithInterests,
        combinedIds
    ) { sp, uwi, ids ->
        val friendsSet: Set<String> = ids.first.toSet()
        val pendingSet = ids.second.toSet()
        val blockedSet = ids.third.toSet()

        val myInterests = sp.interests.map {it.id}

        val sorted = uwi.sortedByDescending { pair ->
            val interests = pair.second.map { it.id }
            cosineSimilarity(interests.toSet(), myInterests.toSet())
        }
        sorted.mapNotNull { pair ->
            val user = pair.first
            val status: FriendshipStatus = when (user?.id ?: return@mapNotNull null) {
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

    init {
        viewModelScope.launch {
            myInterests.collect {
                _defaultSearchProfile.value = searchProfileRepository.getDefaultSearchProfile()
            }
        }

        viewModelScope.launch {
            foundDeviceIds.collect { currentIds ->
                val existingProfileIds = _userRepositories.value.map { it.id }.toSet()
                val idsToFetch = currentIds.filter { it !in existingProfileIds }
                if (idsToFetch.isNotEmpty()) {
                    val newProfiles = idsToFetch.map { id ->
                        val otherUserRepository: OtherUserRepository by inject(OtherUserRepository::class.java) { parametersOf(id) }
                        otherUserRepository
                    }
                    _userRepositories.update { oldProfiles ->
                        val updatedOldProfiles = oldProfiles.filter { it.id in currentIds }
                        updatedOldProfiles + newProfiles
                    }
                } else {
                    _userRepositories.update { oldProfiles ->
                        oldProfiles.filter { it.id in currentIds }
                    }
                }
            }
        }
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

    fun addSearchProfile(name: String, interests: Set<Interest>) {
        _searchProfileLoading.value = true
        viewModelScope.launch {
            val toAdd = SearchProfile(name, interests.toList())
            val result = searchProfileRepository.addSearchProfile(toAdd)
            when (result) {
                is Result.Error -> {
                    _searchProfileError.value = application.applicationContext.getString(result.error.toStringRes())
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
                is Result.Error -> {
                    _searchProfileError.value = application.applicationContext.getString(result.error.toStringRes())
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
                is Result.Error -> {
                    _searchProfileError.value = application.applicationContext.getString(result.error.toStringRes())
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