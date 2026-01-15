package com.example.projektiop.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.FriendItem
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.ThemePreference
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.DataError
import com.example.projektiop.domain.Result
import com.example.projektiop.ui.toStringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(private val appContext: Context,
                        private val themePreference: ThemePreference,
                        private val userRepository: UserRepository,
                        private val friendshipRepository: FriendshipRepository,
                        private val authRepository: AuthRepository): ViewModel() {

    private val _darkMode = MutableStateFlow<Boolean>(themePreference.isDark())
    val darkMode = _darkMode.asStateFlow()

    private val _blockedFriendItems: MutableStateFlow<List<FriendItem>> = MutableStateFlow(emptyList())
    val blockedFriendItems: StateFlow<List<FriendItem>> = _blockedFriendItems.asStateFlow()

    private val _myUserId = MutableStateFlow<String?>(null)
    val myUserId: StateFlow<String?> = _myUserId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _processingIds = MutableStateFlow<List<String>>(emptyList())
    val processingIds: StateFlow<List<String>> = _processingIds.asStateFlow()

    val rememberMe = authRepository.rememberMe

    init {
        viewModelScope.launch {
            userRepository.myUser.collect { updatedUser ->
                _myUserId.value = updatedUser?.id
            }
        }
        viewModelScope.launch {
            friendshipRepository.blockedIds.collect { blockedList ->
                _blockedFriendItems.value = blockedList.mapNotNull { blockedId ->
                    val result = friendshipRepository.getLocalFriendItemByFriendId(blockedId)
                    when (result) {
                        is Result.Error -> {
                                _errorMessage.value = appContext.getString(result.error.toStringRes())
                                return@mapNotNull null
                        }
                        is Result.Success -> {
                            result.data
                        }
                    }
                }
            }
        }
    }


    fun onUnblockClick(friendId: String, friendshipId: String) {
        _processingIds.update { list -> list + friendId }
        viewModelScope.launch {
            friendshipRepository.unblockFriendship(friendshipId)
            _processingIds.update { list -> list - friendId }
        }
    }


    fun onToggleDark() {
        _darkMode.value = !darkMode.value
        themePreference.setDark(darkMode.value)
    }


    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun rememberMe() {
        viewModelScope.launch {
            authRepository.rememberMe()
        }
    }

    // TODO better error messages, success message
    fun onDeleteAccountClick() {
        viewModelScope.launch {
            userRepository.deleteMyAccount().onFailure { e ->
                _errorMessage.value = e.message
            }.onSuccess {
                logout()
            }
        }
    }
}