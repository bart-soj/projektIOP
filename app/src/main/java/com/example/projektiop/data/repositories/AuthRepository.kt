package com.example.projektiop.data.repositories

import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.RegisterRequest
import com.example.projektiop.data.api.LoginRequest
import com.example.projektiop.util.DataError
import retrofit2.HttpException
import com.example.projektiop.util.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


sealed interface AuthEvent {
    data object Success: AuthEvent
    data object Logout: AuthEvent
    data class Error(val message: String) : AuthEvent
}


object AuthRepository {
    private val KEY_TOKEN = "auth_token"
    private val EMAIL = "my_email"
    private val KEY_REMEMBER = "remember_me"

    private val _authEvent = MutableSharedFlow<AuthEvent>()
    val authEvent = _authEvent.asSharedFlow()

    private var token: String? = null

    private val _rememberMe: MutableStateFlow<Boolean> = MutableStateFlow(SharedPreferencesRepository.get(KEY_REMEMBER, false))
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    suspend fun init(){
        if (rememberMe.value) {
            val token = SharedPreferencesRepository.get(KEY_TOKEN, "")
            if (token.isNotEmpty() == true) { // TODO() better token validation
                _authEvent.emit(AuthEvent.Success)
            }
        }
    }


    suspend fun login(email: String, password: String): Result<String?, DataError> {
        return try {
            val response = RetrofitInstance.authApi.login(LoginRequest(email, password))
            saveInfo(email, response.body()?._id) // TODO() handle saving errors
            token = response.body()?.token
            if (!token.isNullOrBlank()) saveToken(token, remember = rememberMe.value) else throw Exception("bad token") // TODO() better token validation
            _authEvent.emit(AuthEvent.Success)
            Result.Success(token)
        } catch (e: HttpException) {
            when(e.code()) {
                401 -> Result.Error(DataError.Authentication.INVALID_EMAIL_PASSWORD)
                403 -> if (e.message?.contains("Please verify your email address before logging in.") == true) {
                    Result.Error(DataError.Authentication.EMAIL_NOT_VERIFIED)
                } else if (e.message?.contains("Your account has been banned.") == true) {
                    Result.Error(DataError.Authentication.ACCOUNT_BANNED)
                } else
                    Result.Error(DataError.Network.UNKNOWN)
                408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
                413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
                429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
                in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
                else -> Result.Error(DataError.Network.UNKNOWN)
            }
        } catch (e: SocketTimeoutException) {
            Result.Error(DataError.Network.REQUEST_TIMEOUT)

        } catch (e: UnknownHostException) {
            Result.Error(DataError.Network.NO_INTERNET)

        } catch (e: IOException) {
            Result.Error(DataError.Network.NO_INTERNET)

        } catch (e: SerializationException) {
            Result.Error(DataError.Network.SERIALIZATION)

        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }
    }


    suspend fun register(username: String, email: String, password: String): Result<String?, DataError> {
        return try {
            val response = RetrofitInstance.authApi.register(RegisterRequest(username, email, password))
            saveInfo(email, response.body()?._id) // TODO() handle saving errors
            token = response.body()?.token
            if (!token.isNullOrBlank()) saveToken(token, remember = rememberMe.value) else throw Exception("bad token") // TODO() better token validation
            _authEvent.emit(AuthEvent.Success)
            Result.Success(token)
        } catch (e: HttpException) {
            when(e.code()) {
                401 -> Result.Error(DataError.Authentication.INVALID_EMAIL_PASSWORD)
                403 -> if (e.message?.contains("Please verify your email address before logging in.") == true) {
                    Result.Error(DataError.Authentication.EMAIL_NOT_VERIFIED)
                } else if (e.message?.contains("Your account has been banned.") == true) {
                    Result.Error(DataError.Authentication.ACCOUNT_BANNED)
                } else
                    Result.Error(DataError.Network.UNKNOWN)
                408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
                413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
                429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
                in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
                else -> Result.Error(DataError.Network.UNKNOWN)
            }
        } catch (e: SocketTimeoutException) {
            Result.Error(DataError.Network.REQUEST_TIMEOUT)

        } catch (e: UnknownHostException) {
            Result.Error(DataError.Network.NO_INTERNET)

        } catch (e: IOException) {
            Result.Error(DataError.Network.NO_INTERNET)

        } catch (e: SerializationException) {
            Result.Error(DataError.Network.SERIALIZATION)

        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }
    }


    suspend fun logout() {
        clearToken()
        _authEvent.emit(AuthEvent.Logout)
    }

    fun rememberMe() {
        _rememberMe.value = !rememberMe.value
        SharedPreferencesRepository.set(KEY_REMEMBER, rememberMe.value)
    }


    fun saveToken(newToken: String?, remember: Boolean) {
        if (!newToken.isNullOrBlank()) {
            SharedPreferencesRepository.set(KEY_TOKEN, newToken)
            SharedPreferencesRepository.set(KEY_REMEMBER, remember)
        } else {
            SharedPreferencesRepository.set(KEY_REMEMBER, false)
            SharedPreferencesRepository.remove(KEY_TOKEN)
        }
    }


    fun saveInfo(newEmail: String?, newId: String?) {
        if (!newEmail.isNullOrBlank()) {
            SharedPreferencesRepository.set(EMAIL, newEmail)
        }
        if (!newId.isNullOrBlank()) {
            SharedPreferencesRepository.set("_id", newId)
        }
        UserRepository.updateMyId()
    }


    fun clearToken() {
        SharedPreferencesRepository.set(KEY_REMEMBER, false)
        SharedPreferencesRepository.remove(KEY_TOKEN)
    }
}
