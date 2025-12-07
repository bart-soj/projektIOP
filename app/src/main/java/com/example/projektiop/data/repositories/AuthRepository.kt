package com.example.projektiop.data.repositories

import android.content.Context
import arrow.core.Either
import arrow.core.raise.result
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.RegisterRequest
import com.example.projektiop.data.api.LoginRequest
import com.example.projektiop.data.api.AuthFailedDto
import com.example.projektiop.util.DataError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import com.example.projektiop.util.Result
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


object AuthRepository {
    private val KEY_TOKEN = "auth_token"
    private val EMAIL = "my_email"
    private val KEY_REMEMBER = "remember_me"

    suspend fun login(email: String, password: String): Result<String?, DataError> {
        return try {
            val response = RetrofitInstance.authApi.login(LoginRequest(email, password))
            saveInfo(email, response.body()?._id) // TODO() handle saving errors
            Result.Success(response.body()?.token)
        } catch (e: HttpException) {
            when(e.code()) {
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
            Result.Success(response.body()?.token)
        } catch (e: HttpException) {
            when(e.code()) {
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


    fun saveToken(newToken: String?, remember: Boolean) {
        if (remember && !newToken.isNullOrBlank()) {
            SharedPreferencesRepository.set(KEY_TOKEN, newToken)
            SharedPreferencesRepository.set(KEY_REMEMBER, true)
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
