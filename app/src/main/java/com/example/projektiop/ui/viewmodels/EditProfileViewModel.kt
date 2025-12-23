package com.example.projektiop.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditProfileViewModel(interestRepository: InterestRepository, userRepository: UserRepository): ViewModel() {

    val publicInterests = interestRepository.publicInterests
    val myInterests = userRepository.MyUserInterests
    val myUser = userRepository.myUser


    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onUpdateClick() {

    }
}