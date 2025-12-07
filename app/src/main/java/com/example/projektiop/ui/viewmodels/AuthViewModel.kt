package com.example.projektiop.ui.viewmodels

import android.util.Patterns
import com.example.projektiop.util.RootError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow




class AuthViewModel() {

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun passwordValidator(password: String): Boolean {
        val hasMinimumLength = password.length >= 8
        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasMinimumLength && hasLowercase && hasUppercase && hasDigit
    }

    fun emailValidator(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    fun usernameValidator(username: String): Boolean {
        return username.isNotBlank()
    }

    fun validateLoginInputs(email: String, password: String): Boolean {
        return passwordValidator(password) && emailValidator(email)
    }

    fun validateRegisterInputs(username: String, email: String, password: String): Boolean {
        return passwordValidator(password) && emailValidator(email) && usernameValidator(username)
    }


}