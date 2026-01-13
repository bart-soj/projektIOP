package com.example.projektiop.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.domain.AppStateRepository
import com.example.projektiop.util.BackupError
import com.example.projektiop.util.KeyUtils
import com.example.projektiop.util.DataError
import com.example.projektiop.util.Result
import com.example.projektiop.util.backupPasswordValidator
import com.example.projektiop.util.mapToResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KeyLoadingViewModel(private val keyUtils: KeyUtils,
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
        set(value) {
            sharedDataSource.set("backup", value)
        }

    fun ensureKeys() {
        _gettingBackup.value = false
        _errorMessage.value = null
        _gotKeys.value = false
        viewModelScope.launch {
            Log.d("KEYS", "getting local")
            val localResult = keyUtils.getLocalKeyPair(userId)
            when(localResult) {
                is Result.Error -> {}
                is Result.Success -> {
                    var synced = false
                    var failCounter = 0
                    while (synced == false) {
                        if (failCounter >= 8) {
                            synced = true
                            appStateRepository.logout()
                        } // unlikely, force user to log-in again
                        val myKeyResult = keyUtils.getPubKey(userId)
                        when (myKeyResult) {
                            is Result.Error -> { failCounter++ }
                            is Result.Success -> {
                                if (myKeyResult.data != localResult.data.first) {
                                    val postResult = keyUtils.postMyPubKey(userId, localResult.data.first)
                                    when (postResult) {
                                        is Result.Error -> { failCounter++ }
                                        is Result.Success -> {
                                            synced = true
                                            _gotKeys.value = true
                                            Log.d("KYS", "got em!")
                                            appStateRepository.gotKeys()
                                        }
                                    }
                                } else {
                                    synced = true
                                    _gotKeys.value = true
                                    Log.d("KYS", "got em!")
                                    appStateRepository.gotKeys()
                                }
                            }
                        }
                    }
                }
            }

            Log.d("KEYS","checking for backup")
            if (isBackedUp == false && gotKeys.value == false) {
                Log.d("KEYS","creating keys")
                val createResult = keyUtils.createKeyPair(userId)
                when (createResult) {
                    is Result.Error -> {
                        Log.d("KEYS","logging-out cuz failed")
                        appStateRepository.logout()} // unlikely, force user to log-in again
                    is Result.Success -> {
                        Log.d("KEYS", "got em!")
                        _gotKeys.value = true
                        appStateRepository.gotKeys()
                    }
                }
            } else if(gotKeys.value == false) {
                Log.d("KEYS", "getting backup")
                _gettingBackup.value = true
            }
        }
    }

    fun onBackupClick(password: String) {
        _backupLoading.value = true
        viewModelScope.launch {
            val result = keyUtils.getBackupInfo()
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
                        DataError.Authentication.EMAIL_USERNAME_TAKEN -> "Username or Email taken"
                    }

                is Result.Success -> {
                    val decryptionResult = keyUtils.getDecryptedKeyPairFromBackup(password, result.data, userId)
                    when(decryptionResult) {
                        is Result.Error -> _errorMessage.value = when (decryptionResult.error) {
                            BackupError.WRONG_PASSWORD -> "Wrong password!"
                        }
                        is Result.Success -> {
                            _gotKeys.value = true
                            Log.d("KEYS", "got em!")
                            appStateRepository.gotKeys()}
                    }
                }
            }
            _backupLoading.value = false
        }
    }

    fun onForgetClick() {
        isBackedUp = false
        ensureKeys()
    }

    fun onPasswordChange(password: String) {
        _passwordErrors.value = backupPasswordValidator(password).mapToResource()
    }
}
