package com.example.projektiop.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.AuthApi
import com.example.projektiop.data.api.EmailRequest
import com.example.projektiop.domain.DataError
import com.example.projektiop.data.util.apiExceptionToDataError
import com.example.projektiop.util.emailValidator
import com.example.projektiop.ui.mapToResource
import com.example.projektiop.ui.toStringRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface ResendUiEvent{
    data object ResendSuccess : ResendUiEvent
    data object ResendError : ResendUiEvent
    data object ShowToast : ResendUiEvent
}

class ResendEmailViewModel(private val appContext: Context,
                           private val emailApi: AuthApi) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _emailErrors: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val emailErrors: StateFlow<List<Int>> = _emailErrors.asStateFlow()

    private val delayLengthMS = 60000
    private val _delaying = MutableStateFlow<Int?>(null)
    val delaying: StateFlow<Int?> = _delaying.asStateFlow()


    private val _uiEvents = Channel<ResendUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun onResendClick(email: String) {
        _loading.value = true

        viewModelScope.launch {

            val request = EmailRequest(email)

            try {
                val result = emailApi.resendVerificationEmail(request)
                if (!result.isSuccessful) throw HttpException(result)
                _uiEvents.send(ResendUiEvent.ResendSuccess)
            } catch(e: Exception) {
                val error = apiExceptionToDataError<Unit>(e).error
                _errorMessage.value = appContext.getString(error.toStringRes())
            }
            _loading.value = false
        }
    }

    fun onSuccessWait() {
        _delaying.value = delayLengthMS / 1000
        var timeLeft =  delayLengthMS
        viewModelScope.launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft -= 1000
                _delaying.value = if (timeLeft <= 0) null else timeLeft / 1000
            }
        }
    }

    fun onEmailChange(email: String) {
        _emailErrors.value = emailValidator(email).mapToResource()
    }
}