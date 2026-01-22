package com.example.projektiop.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.OtherUserRepository
import com.example.projektiop.domain.models.User
import com.example.projektiop.domain.models.User.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendProfileViewModel(private val id: String,
                             private val username: String? = null,
                             private val displayName: String? = null,
                             private val avatarUrl: String? = null,
                             private val userRepository: OtherUserRepository) : ViewModel() {

    private val prefill: User = User(
        id = id,
        username = username.toString(),
        profile = UserProfile(
            displayName = (displayName ?: username).toString(),
            avatarUrl = avatarUrl.toString()
        ),
        email = ""
    )

    val profile = userRepository.Profile.map { it ?: prefill }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = prefill
        )

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val interests = userRepository.UserInterests


    init {
        refresh()
    }

    fun refresh() {
        _loading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = userRepository.fetchProfile()
            _loading.value = false
        }
    }
}