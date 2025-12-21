package com.example.projektiop.ui.viewmodels

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.domain.AppStateRepository
import com.example.projektiop.domain.models.base64
import com.example.projektiop.util.BackupError
import com.example.projektiop.util.CertificateUtils
import com.example.projektiop.util.DataError
import com.example.projektiop.util.Result
import com.example.projektiop.util.ValidationError
import com.example.projektiop.util.ValidationError.Common
import com.example.projektiop.util.ValidationError.EmailError
import com.example.projektiop.util.ValidationError.PasswordError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KeyLoadingViewModel(private val certificateUtils: CertificateUtils,
                          private val authRepository: AuthRepository,
                          private val appStateRepository: AppStateRepository,
                          private val sharedDataSource: SharedDataSource) : ViewModel() {

    private val _backupLoading = MutableStateFlow(false)
    val backupLoading: StateFlow<Boolean> = _backupLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _gotKeys = MutableStateFlow<Boolean>(false)
    val gotKeys: StateFlow<Boolean> = _gotKeys.asStateFlow()

    private val _gettingBackup = MutableStateFlow<Boolean>(false)
    val gettingBackup: StateFlow<Boolean> = _gettingBackup.asStateFlow()

    private val _passwordErrors: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val passwordErrors: StateFlow<List<String>> = _passwordErrors.asStateFlow()

    private var userId: String
        get() = sharedDataSource.get("_id", "")
        set(value) {}

    private var isBackedUp: Boolean
        get() = sharedDataSource.get("backup", false)
        set(value) {}

    fun ensureKeys() {
        _gettingBackup.value = false
        _errorMessage.value = null
        viewModelScope.launch {
            val localResult = certificateUtils.getLocalKeyPair(userId)
            when(localResult) {
                is Result.Error -> {}
                is Result.Success -> {appStateRepository.gotKeys()}
            }

            if (isBackedUp == false) {
                val createResult = certificateUtils.createKeyPair(userId)
                when (createResult) {
                    is Result.Error -> {appStateRepository.logout()} // unlikely, force user to log-in again
                    is Result.Success -> {appStateRepository.gotKeys()}
                }
            }

            _gettingBackup.value = true
        }
    }

    fun onBackupClick(password: String) {
        _backupLoading.value = true
        viewModelScope.launch {
            val result = certificateUtils.getBackupInfo()
            when(result) {
                is Result.Error ->
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
                    }

                is Result.Success -> {
                    val decryptionResult = certificateUtils.getDecryptedKeyPairFromBackup(password, result.data, userId)
                    when(decryptionResult) {
                        is Result.Error -> _errorMessage.value = when (decryptionResult.error) {
                            BackupError.WRONG_PASSWORD -> "Wrong password!"
                        }
                        is Result.Success -> {appStateRepository.gotKeys()}
                    }
                }
            }
        }
    }

    fun onPasswordChange(password: String) {
        _passwordErrors.value = passwordValidator(password).mapToString()
    }

    fun passwordValidator(password: String): List<ValidationError> {
        var out = emptyList<ValidationError>()
        val hasMinimumLength = password.length >= 12
        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }

        if (!hasMinimumLength) out += PasswordError.TOO_SHORT
        if (!hasLowercase) out += PasswordError.NO_LOWERCASE
        if (!hasUppercase) out += PasswordError.NO_UPPERCASE
        if (!hasDigit) out += PasswordError.NO_DIGIT

        return out
    }

    fun List<ValidationError>.mapToString(): List<String> {
        return this.map { item ->
            when (item) {
                Common.BLANK -> "mustn't be empty"
                EmailError.NOT_EMAIL -> "must be an email"
                PasswordError.TOO_SHORT -> "must be longer than 11 character"
                PasswordError.NO_UPPERCASE -> "must contain at least one uppercase letter"
                PasswordError.NO_DIGIT -> "must contain at least one letter"
                PasswordError.NO_LOWERCASE -> "must contain at least one lowercase letter"
                else -> "unknown error"
            }
        }
    }
}
