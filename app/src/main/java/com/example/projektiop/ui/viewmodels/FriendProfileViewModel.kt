package com.example.projektiop.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.ProfileDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendProfileViewModel(private val id: String,
                             private val userRepository: UserRepository) : ViewModel() {

    private val _user = MutableStateFlow<UserProfileResponse?>(null)
    val user = _user.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var prefil: UserProfileResponse = UserProfileResponse()

    fun refresh() {
        _loading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = userRepository.fetchUserById(id)
            result.onSuccess {
                _user.value = result.getOrNull()
            }.onFailure {
                _user.value = prefil
                // _errorMessage.value = "Failed to refresh"
            }
            _loading.value = false
        }
    }

    fun setPrefil(
        usernamePrefill: String? = null,
        displayNamePrefill: String? = null,
        avatarUrlPrefill: String? = null
    ) {
        prefil = UserProfileResponse(
            _id = id,
            username = usernamePrefill?: prefil.username,
            profile = ProfileDto( displayName = displayNamePrefill ?: prefil.profile?.displayName,
                                  avatarUrl = avatarUrlPrefill ?: prefil.profile?.avatarUrl)
        )
    }
}

/*  code that was in the composable before, TODO() maybe use the search users in viewModel
scope.launch {
    loading = true
    error = null
    try {
        val fullResp = RetrofitInstance.userApi.getUserById(userId)
        if (fullResp.isSuccessful && fullResp.body() != null) {
            profile = fullResp.body()
        } else {
            if (profile == null) {
                if (displayNamePrefill != null || usernamePrefill != null) {
                    profile = UserProfileResponse(
                        _id = userId,
                        username = usernamePrefill,
                        profile = ProfileDto(displayName = displayNamePrefill),
                        interests = emptyList(),
                        email = null
                    )
                }
                val uname = usernamePrefill ?: displayNamePrefill
                if (!uname.isNullOrBlank()) {
                    val searchResp = RetrofitInstance.userApi.searchUsers(uname)
                    if (searchResp.isSuccessful) {
                        val candidate = searchResp.body().orEmpty().firstOrNull { it._id == userId || it.username == uname }
                        if (candidate != null) {
                            profile = UserProfileResponse(
                                _id = candidate._id,
                                username = candidate.username,
                                profile = candidate.profile,
                                interests = emptyList(),
                                email = null
                            )
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        error = e.message
    }
    loading = false
}
 */