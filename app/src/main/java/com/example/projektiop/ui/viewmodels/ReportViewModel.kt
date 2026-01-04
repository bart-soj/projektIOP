package com.example.projektiop.ui.viewmodels

import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.CreateReportRequest
import com.example.projektiop.data.api.CreateReportResponse
import com.example.projektiop.data.api.ReportApi
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.domain.ReportType
import com.example.projektiop.util.DataError
import com.example.projektiop.util.apiExceptionToDataError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ReportUiEvent{
    data object ReportSuccess: ReportUiEvent
    data object ReportError: ReportUiEvent
    data object ShowToast : ReportUiEvent
}

class ReportViewModel(private val sharedDataSource: SharedDataSource,
                      private val reportApi: ReportApi) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val userId: String
        get() = sharedDataSource.get("_id", "")

    private val _uiEvents = Channel<ReportUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun onReportClick(userId: String, messageId: String?, reportType: ReportType, reason: String) {
        _loading.value = true

        viewModelScope.launch {
            if (userId.isBlank() && messageId.isNullOrBlank() ) {
                _errorMessage.value = "need either a message or user id"
                _loading.value = false
                return@launch
            }

            val request = CreateReportRequest(
                reportedUserId = userId,
                reportedMessageId = messageId.takeIf { it?.isNotBlank() == true },
                reportType = reportType.name.lowercase(),
                reason = reason
            )

            try {
                val result = reportApi.createReport(request)
                _uiEvents.send(ReportUiEvent.ReportSuccess)
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
                }
            }
            _loading.value = false
        }
    }
}