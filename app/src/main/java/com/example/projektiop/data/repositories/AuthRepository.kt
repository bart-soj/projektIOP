package com.example.projektiop.data.repositories

import com.example.projektiop.data.api.AuthApi
import com.example.projektiop.data.api.RegisterRequest
import com.example.projektiop.data.api.LoginRequest
import com.example.projektiop.data.api.TokenProvider
import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.domain.AppStateRepository
import com.example.projektiop.domain.DataError
import com.example.projektiop.domain.Result
import com.example.projektiop.data.util.apiExceptionToDataError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException


sealed interface AuthState {
    object Unauthenticated: AuthState
    data class Authenticated(
        val userId: String,
        val isBackedUp: Boolean
    ): AuthState
}


class AuthRepository(private val authApi: AuthApi,
                     private val sharedDataSource: SharedDataSource,
                     private val appStateRepository: AppStateRepository,
                     private val dbRepository: RealmDBRepository): TokenProvider {
    private val KEY_TOKEN = "auth_token"
    private val KEY_EMAIL = "my_email"
    private val KEY_REMEMBER = "remember_me"
    private val KEY_ID = "_id"
    private val KEY_BACKUP = "backup"

    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var token: String? = null

    private val _rememberMe: MutableStateFlow<Boolean> = MutableStateFlow(sharedDataSource.get(KEY_REMEMBER, false))
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            if (rememberMe.value == true) {
                val tmpToken = sharedDataSource.get(KEY_TOKEN, "")
                val id = sharedDataSource.get(KEY_ID, "")
                val isBackedUp = sharedDataSource.get(KEY_BACKUP, false)
                if (validateToken(tmpToken) && id.isNotBlank()) {
                    token = tmpToken
                    _authState.value = AuthState.Authenticated(id, isBackedUp)
                    appStateRepository.login()
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    suspend fun login(email: String, password: String): Result<String?, DataError> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val tmpId = response.body()?._id
                val tmpToken = response.body()?.token
                val tmpEmail = response.body()?.email
                val isBackedUp = response.body()?.isBackedUp
                if (validateToken(tmpToken) && !tmpId.isNullOrBlank()
                    && !tmpEmail.isNullOrBlank() && isBackedUp != null
                ) {
                    saveToken(tmpToken, remember = rememberMe.value)
                    saveInfo(email, tmpId, isBackedUp)
                } else throw Exception("bad data")
                _authState.value = AuthState.Authenticated(tmpId, isBackedUp)
                appStateRepository.login()
                Result.Success(token)
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            return apiExceptionToDataError<String?>(e)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<Unit, DataError> {
        return try {
            val response = authApi.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                throw HttpException(response)
            }
        }  catch (e: Exception) {
            return apiExceptionToDataError<Unit>(e)
        }
    }

    suspend fun logout() {
        onLogoutCleanup()
        _authState.value = AuthState.Unauthenticated
        appStateRepository.logout()
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


    fun saveInfo(newEmail: String, newId: String, isBackedUp: Boolean) {
        if (newEmail.isNotBlank()) {
            sharedDataSource.set(KEY_EMAIL, newEmail)
        }
        if (newId.isNotBlank()) {
            sharedDataSource.set(KEY_ID, newId)
        }
        sharedDataSource.set(KEY_BACKUP, isBackedUp)
    }

    suspend fun onTerminate() {
        if (!rememberMe.value) {
            onLogoutCleanup()
        }
    }

    suspend fun onLogoutCleanup() {
        dbRepository.deleteContents()
        token = null
        sharedDataSource.set(KEY_REMEMBER, false)
        sharedDataSource.remove(KEY_TOKEN)
        sharedDataSource.remove(KEY_ID)
        sharedDataSource.remove(KEY_BACKUP)
        sharedDataSource.remove(KEY_EMAIL)
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
