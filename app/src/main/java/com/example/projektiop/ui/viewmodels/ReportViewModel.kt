package com.example.projektiop.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.R
import com.example.projektiop.data.api.CreateReportRequest
import com.example.projektiop.data.api.ReportApi
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.domain.ReportType
import com.example.projektiop.domain.DataError
import com.example.projektiop.data.util.apiExceptionToDataError
import com.example.projektiop.ui.toStringRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface ReportUiEvent{
    data object ReportSuccess: ReportUiEvent
    data object ReportError: ReportUiEvent
    data object ShowToast : ReportUiEvent
}

class ReportViewModel(private val appContext: Context,
                      private val sharedDataSource: SharedDataSource,
                      private val reportApi: ReportApi) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val userId: String
        get() = sharedDataSource.get("_id", "")

    private val _uiEvents = Channel<ReportUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun onReportClick(userId: String, messageId: String?, content: String?,
                      reportType: ReportType, reason: String) {
        _loading.value = true

        viewModelScope.launch {
            if (userId.isBlank() && messageId.isNullOrBlank() ) {
                _errorMessage.value = appContext.getString(R.string.error)
                _loading.value = false
                return@launch
            }

            val request = CreateReportRequest(
                reportedUserId = userId,
                reportedMessageId = messageId.takeIf { it?.isNotBlank() == true },
                reportType = reportType.name.lowercase(),
                reason = if (!content.isNullOrBlank()) {
                    "<content>${content}</content>" + reason
                } else {
                    reason
                }
            )

            try {
                val result = reportApi.createReport(request)
                if (!result.isSuccessful) throw HttpException(result)
                _uiEvents.send(ReportUiEvent.ReportSuccess)
            } catch(e: Exception) {
                val error = apiExceptionToDataError<Unit>(e).error
                _errorMessage.value = appContext.getString(error.toStringRes())
            }
            _loading.value = false
        }
    }
}