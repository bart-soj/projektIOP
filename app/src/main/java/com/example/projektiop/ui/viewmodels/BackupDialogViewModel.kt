package com.example.projektiop.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.BackupApi
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.data.util.KeyUtils
import com.example.projektiop.domain.DataError
import com.example.projektiop.data.util.apiExceptionToDataError
import com.example.projektiop.domain.backupPasswordValidator
import com.example.projektiop.ui.mapToResource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface BackupDialogUiEvent {
    data object BackupSuccess : BackupDialogUiEvent
    data object BackupError : BackupDialogUiEvent
    data object ShowToast : BackupDialogUiEvent
}

class BackupDialogViewModel(private val sharedDataSource: SharedDataSource,
                            private val backupApi: BackupApi,
                            private val keyUtils: KeyUtils) : ViewModel() {

    private val _passwordErrors: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val passwordErrors: StateFlow<List<Int>> = _passwordErrors.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val userId: String
        get() = sharedDataSource.get("_id", "")

    val isBackedUp: Boolean
        get() = sharedDataSource.get("backup", false)

    private val _uiEvents = Channel<BackupDialogUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun onPasswordChange(password: String) {
        _passwordErrors.value = backupPasswordValidator(password).mapToResource()
    }

    fun onSetClick(password: String) {
        _loading.value = true
        viewModelScope.launch {

            val backupInfo = keyUtils.createBackupInfo(password, userId)

            try {
                val result = backupApi.saveBackup(backupInfo)
                if (!result.isSuccessful) throw HttpException(result)
                sharedDataSource.set("backup", true)
                _uiEvents.send(BackupDialogUiEvent.BackupSuccess)
            } catch(e: Exception) {
                val error = apiExceptionToDataError<Unit>(e).error
                _errorMessage.value = when(error) {
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
            _loading.value = false
        }
    }
}