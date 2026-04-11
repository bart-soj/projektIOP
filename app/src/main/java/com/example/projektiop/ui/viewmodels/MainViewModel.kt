package com.example.projektiop.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.models.UserInterest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private const val ID: String = "_id"


class MainViewModel(private val userRepository: UserRepository) : ViewModel() {

    val myInterests: StateFlow<List<UserInterest>?> = userRepository.MyUserInterests

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()


    val user = userRepository.myUser

    init {
        fetchMyProfile()
    }

    fun fetchMyProfile() {
        _loading.value = true
        _errorMessage.value = null
        viewModelScope.launch{
            try {
                userRepository.fetchMyProfile()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun getMyProfile() { // always uses local data if possible
        _loading.value = true
        _errorMessage.value = null
        viewModelScope.launch{
            try {
                userRepository.getMyProfile()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
}