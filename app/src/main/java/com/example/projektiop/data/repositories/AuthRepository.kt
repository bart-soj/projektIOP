package com.example.projektiop.data.repositories

import com.example.projektiop.data.api.AuthApi
import com.example.projektiop.data.api.RegisterRequest
import com.example.projektiop.data.api.LoginRequest
import com.example.projektiop.data.api.TokenProvider
import com.example.projektiop.util.DataError
import retrofit2.HttpException
import com.example.projektiop.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


sealed interface AuthEvent {
    data object Success: AuthEvent
    data object Logout: AuthEvent
    data class Error(val message: String) : AuthEvent
}


sealed interface AuthState {
    object Unauthenticated: AuthState
    object Loading: AuthState
    data class Authenticated(
        val userId: String,
    ): AuthState
}


class AuthRepository(private val authApi: AuthApi,
                     private val sharedDataSource: SharedDataSource,
                     private val userRepository: UserRepository): TokenProvider {
    private val KEY_TOKEN = "auth_token"
    private val KEY_EMAIL = "my_email"
    private val KEY_REMEMBER = "remember_me"
    private val KEY_ID = "_id"

    private val _authEvent = MutableSharedFlow<AuthEvent>()
    val authEvent = _authEvent.asSharedFlow()

    val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var token: String? = null

    private val _rememberMe: MutableStateFlow<Boolean> = MutableStateFlow(sharedDataSource.get(KEY_REMEMBER, false))
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    init {
        if (rememberMe.value) {
            val tmpToken = sharedDataSource.get(KEY_TOKEN, "")
            val id = sharedDataSource.get(KEY_ID, "")
            if (validateToken(tmpToken) && id.isNotBlank()) {
                token = tmpToken
                _authState.value = AuthState.Authenticated(id)
                CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                    _authEvent.emit(AuthEvent.Success)
                }
            }
        }
    }


    suspend fun login(email: String, password: String): Result<String?, DataError> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            val tmpId = response.body()?._id
            val tmpToken = response.body()?.token
            val tmpEmail = response.body()?.email
            if (validateToken(tmpToken) && !tmpId.isNullOrBlank() && !tmpEmail.isNullOrBlank()) {
                saveToken(tmpToken, remember = rememberMe.value)
                saveInfo(email, tmpId) // TODO() handle saving errors
            } else throw Exception("bad data")
            _authState.value = AuthState.Authenticated(tmpId)
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
            Result.Error(DataError.Network.SERVER_ERROR)
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
            val response = authApi.register(RegisterRequest(username, email, password))
            val tmpId = response.body()?._id
            val tmpToken = response.body()?.token
            val tmpEmail = response.body()?.email
            if (validateToken(tmpToken) && !tmpId.isNullOrBlank() && !tmpEmail.isNullOrBlank()) {
                saveToken(tmpToken, remember = rememberMe.value)
                saveInfo(email, tmpId) // TODO() handle saving errors
            } else throw Exception("bad data")
            _authState.value = AuthState.Authenticated(tmpId)
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
            Result.Error(DataError.Network.SERVER_ERROR)
        } catch (e: IOException) {
            Result.Error(DataError.Network.NO_INTERNET)

        } catch (e: SerializationException) {
            Result.Error(DataError.Network.SERIALIZATION)

        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }
    }


    suspend fun logout() {
        _authState.value = AuthState.Loading
        clearToken()
        _authEvent.emit(AuthEvent.Logout)
        _authState.value = AuthState.Unauthenticated
    }

    fun rememberMe() {
        _rememberMe.value = !rememberMe.value
        sharedDataSource.set(KEY_REMEMBER, rememberMe.value)
    }


    fun saveToken(newToken: String?, remember: Boolean) {
        if (!newToken.isNullOrBlank()) {
            token = newToken
            sharedDataSource.set(KEY_TOKEN, newToken)
            sharedDataSource.set(KEY_REMEMBER, remember)
        } else {
            sharedDataSource.set(KEY_REMEMBER, false)
            sharedDataSource.remove(KEY_TOKEN)
        }
    }


    fun saveInfo(newEmail: String?, newId: String?) {
        if (!newEmail.isNullOrBlank()) {
            sharedDataSource.set(KEY_EMAIL, newEmail)
        }
        if (!newId.isNullOrBlank()) {
            sharedDataSource.set(KEY_ID, newId)
        }
        userRepository.updateMyId()
    }

    fun onTerminate() {
        if (!rememberMe.value) {
            clearToken()
        }
    }


    fun clearToken() {
        token = null
        sharedDataSource.set(KEY_REMEMBER, false)
        sharedDataSource.remove(KEY_TOKEN)
    }

    override fun getToken(): String? {
        return token
    }

    fun validateToken(token: String?): Boolean {

        if (token.isNullOrBlank()) return false

        val parts = token.split('.')
        if (parts.size != 3) return false

        val base64UrlRegex = Regex("^[A-Za-z0-9_-]+$")

        return parts.all { it.isNotEmpty() && base64UrlRegex.matches(it) }

        return true
    }
}
