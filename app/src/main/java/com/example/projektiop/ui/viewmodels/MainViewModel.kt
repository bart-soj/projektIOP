package com.example.projektiop.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private const val ID: String = "_id"


class MainViewModel(private val userRepository: UserRepository) : ViewModel() {

    val myInterests: StateFlow<List<UserInterestDto>?> = userRepository.MyUserInterests
    private val _loading = MutableStateFlow(false)

    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.myUserFlow.collect { updatedUser ->
                _user.value = updatedUser
            }
        }
    }

    fun refreshProfile() {
        _loading.value = true
        viewModelScope.launch{
            try {
                userRepository.fetchMyProfile()
            } catch (e: Exception) {
                Log.e("ProfileRefresh", "Error fetching profile", e)
            } finally {
                _loading.value = false
            }
        }
    }
}