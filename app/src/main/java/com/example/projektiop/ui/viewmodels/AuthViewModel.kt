package com.example.projektiop.ui.viewmodels

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.util.DataError
import com.example.projektiop.util.Result
import com.example.projektiop.util.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.projektiop.util.ValidationError.Common
import com.example.projektiop.util.ValidationError.PasswordError
import com.example.projektiop.util.emailValidator
import com.example.projektiop.util.mapToResource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface AuthUiEvent{
    data class NavigateToResend(val email: String? = null) : AuthUiEvent
}

class AuthViewModel(val authRepository: AuthRepository): ViewModel() {

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val rememberMe: StateFlow<Boolean> = authRepository.rememberMe

    private val _inputsValid: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val inputsValid: StateFlow<Boolean> = _inputsValid.asStateFlow()

    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _passwordErrors: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val passwordErrors: StateFlow<List<Int>> = _passwordErrors.asStateFlow()

    private val _emailErrors: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val emailErrors: StateFlow<List<Int>> = _emailErrors.asStateFlow()

    private val _usernameErrors: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val usernameErrors: StateFlow<List<Int>> = _usernameErrors.asStateFlow()

    private val _authUiChannel = Channel<AuthUiEvent>()
    val authUiFlow = _authUiChannel.receiveAsFlow()

    fun rememberMe() {
        authRepository.rememberMe()
    }

    fun onPasswordChange(password: String) {
        _passwordErrors.value = passwordValidator(password).mapToResource()
        _inputsValid.value = validateInputs()
    }

    fun onEmailChange(email: String) {
        _emailErrors.value = emailValidator(email).mapToResource()
        _inputsValid.value = validateInputs()
    }

    fun onUsernameChange(username: String) {
        _usernameErrors.value = usernameValidator(username).mapToResource()
        _inputsValid.value = validateInputs()
    }

    fun onLoginClick(email: String, password: String) {
        if (loading.value) return
        if (inputsValid.value) {
            _loading.value = true
            _errorMessage.value = null
            viewModelScope.launch {
                val result = authRepository.login(email, password)
                when(result) {
                    is Result.Error -> {
                         _errorMessage.value = when (result.error) {
                            DataError.Local.DISK_FULL -> "no disk space"
                            DataError.Local.DB_ERROR -> "db failed"
                            DataError.Network.REQUEST_TIMEOUT -> "request timeout"
                            DataError.Network.TOO_MANY_REQUESTS -> "Server Error"
                            DataError.Network.NO_INTERNET -> "no internet"
                            DataError.Network.PAYLOAD_TOO_LARGE -> "Server Error"
                            DataError.Network.SERVER_ERROR -> "Server Error"
                            DataError.Network.SERIALIZATION -> "Serialization Error"
                            DataError.Network.UNKNOWN -> "Unknown Error"
                            DataError.Local.NO_DATA -> "no local data"
                            DataError.Authentication.INVALID_EMAIL_PASSWORD -> "Invalid Email or Password"
                            DataError.Authentication.ACCOUNT_BANNED -> "Account banned"
                            DataError.Authentication.EMAIL_NOT_VERIFIED -> "Email not verified"
                            DataError.Authentication.EMAIL_USERNAME_TAKEN -> "Username or Email taken"
                        }
                    }
                    is Result.Success -> {}
                }
                _loading.value = false
            }
        }
    }

    fun onRegisterClick(username: String, email: String, password: String) {
        if (loading.value) return
        if (inputsValid.value) {
            _loading.value = true
            _errorMessage.value = null

            viewModelScope.launch {
                val result = authRepository.register(username, email, password)

                when (result) {
                    is Result.Error -> {
                        _errorMessage.value = when (result.error) {
                            DataError.Local.DISK_FULL -> "no disk space"
                            DataError.Local.DB_ERROR -> "db failed"
                            DataError.Network.REQUEST_TIMEOUT -> "request timeout"
                            DataError.Network.TOO_MANY_REQUESTS -> "Server Error"
                            DataError.Network.NO_INTERNET -> "no internet"
                            DataError.Network.PAYLOAD_TOO_LARGE -> "Server Error"
                            DataError.Network.SERVER_ERROR -> "Server Error"
                            DataError.Network.SERIALIZATION -> "Serialization Error"
                            DataError.Network.UNKNOWN -> "Unknown Error"
                            DataError.Local.NO_DATA -> "no local data"
                            DataError.Authentication.INVALID_EMAIL_PASSWORD -> "Invalid Email or Password"
                            DataError.Authentication.ACCOUNT_BANNED -> "Account banned"
                            DataError.Authentication.EMAIL_NOT_VERIFIED -> "Email not verified"
                            DataError.Authentication.EMAIL_USERNAME_TAKEN -> "Username or Email taken"
                        }
                    }

                    is Result.Success -> {
                        _authUiChannel.send(AuthUiEvent.NavigateToResend(email))
                    }
                }
                _loading.value = false
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun onResendClick() {
        viewModelScope.launch {
            _authUiChannel.send(AuthUiEvent.NavigateToResend())
        }
    }

    fun passwordValidator(password: String): List<ValidationError> {
        var out = emptyList<ValidationError>()
        val hasMinimumLength = password.length >= 8
        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }

        if (!hasMinimumLength) out += PasswordError.TOO_SHORT
        if (!hasLowercase) out += PasswordError.NO_LOWERCASE
        if (!hasUppercase) out += PasswordError.NO_UPPERCASE
        if (!hasDigit) out += PasswordError.NO_DIGIT

        return out
    }


    fun usernameValidator(username: String): List<ValidationError> {
        var out = emptyList<ValidationError>()

        if(!username.isNotBlank()) out += Common.BLANK

        return out
    }

    fun validateInputs(): Boolean {
        return passwordErrors.value == emptyList<ValidationError>()
                && emailErrors.value == emptyList<ValidationError>()
                && usernameErrors.value == emptyList<ValidationError>()
    }
}