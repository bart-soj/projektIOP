package com.example.projektiop.ui.viewmodels

import android.util.Log
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

    private val _passwordErrors: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val passwordErrors: StateFlow<List<Int>> = _passwordErrors.asStateFlow()

    private var userId: String
        get() = sharedDataSource.get("_id", "")
        set(value) {}

    private var isBackedUp: Boolean
        get() = sharedDataSource.get("backup", false)
        set(value) {}

    fun ensureKeys() {
        _gettingBackup.value = false
        _errorMessage.value = null
        _gotKeys.value = false
        viewModelScope.launch {
            Log.d("KYS", "getting local")
            val localResult = certificateUtils.getLocalKeyPair(userId)
            when(localResult) {
                is Result.Error -> {}
                is Result.Success -> {
                    _gotKeys.value = true
                    Log.d("KYS", "got em!")
                    appStateRepository.gotKeys()
                }
            }

            Log.d("KYS","checking for backup")
            if (isBackedUp == false && gotKeys.value == false) {
                Log.d("KYS","creating keys")
                val createResult = certificateUtils.createKeyPair(userId)
                when (createResult) {
                    is Result.Error -> {
                        Log.d("KYS","logging-out cuz failed")
                        appStateRepository.logout()} // unlikely, force user to log-in again
                    is Result.Success -> {
                        Log.d("KYS", "got em!")
                        _gotKeys.value = true
                        appStateRepository.gotKeys()
                    }
                }
            } else if(gotKeys.value == false) {
                Log.d("KYS", "getting backup")
                _gettingBackup.value = true
            }
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
                        is Result.Success -> {
                            _gotKeys.value = true
                            Log.d("KYS", "got em!")
                            appStateRepository.gotKeys()}
                    }
                }
            }
        }
    }

    fun onPasswordChange(password: String) {
        _passwordErrors.value = passwordValidator(password).mapToResource()
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

    fun List<ValidationError>.mapToResource(): List<Int> {
        return this.map { item ->
            when (item) {
                Common.BLANK ->  com.example.projektiop.R.string.error_field_empty
                EmailError.NOT_EMAIL -> com.example.projektiop.R.string.error_invalid_email
                PasswordError.TOO_SHORT -> com.example.projektiop.R.string.error_backup_password_too_short
                PasswordError.NO_UPPERCASE -> com.example.projektiop.R.string.error_password_no_uppercase
                PasswordError.NO_DIGIT -> com.example.projektiop.R.string.error_password_no_digit
                PasswordError.NO_LOWERCASE -> com.example.projektiop.R.string.error_password_no_lowercase

                else -> com.example.projektiop.R.string.error_unknown
            }
        }
    }
}
