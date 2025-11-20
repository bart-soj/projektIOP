package com.example.projektiop.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private const val ID: String = "_id"


class MainViewModel() : ViewModel() {

    // userId z SharedPreferences
    private val userId: String = SharedPreferencesRepository.get(ID, "brak")

    val myProfile: StateFlow<UserProfileResponse?> = UserRepository.MyProfile
    val myInterests: StateFlow<List<UserInterestDto>?> = UserRepository.MyUserInterests
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun refreshProfile() {
        _loading.value = true
        viewModelScope.launch{
            try {
                UserRepository.fetchMyProfile()
            } catch (e: Exception) {
                Log.e("ProfileRefresh", "Error fetching profile", e)
            } finally {
                _loading.value = false
            }
        }
    }
}