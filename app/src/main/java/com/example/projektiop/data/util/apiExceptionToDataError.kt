package com.example.projektiop.data.util


import android.util.Log
import com.example.projektiop.domain.DataError
import com.example.projektiop.domain.Result
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException
import retrofit2.HttpException
import kotlinx.serialization.SerializationException

fun <T> apiExceptionToDataError(e: Exception): Result.Error<T, DataError> {
    Log.d("ERR", "", e)
    return when (e) {
        is HttpException -> {
            val errorMessage = e.response()?.errorBody()?.string()
            when (e.code()) {
                400 -> Result.Error(DataError.Authentication.EMAIL_USERNAME_TAKEN)
                401 -> {
                  if (errorMessage?.contains("Invalid credentials") == true) {
                      Result.Error(DataError.Authentication.INVALID_EMAIL_PASSWORD)
                  } else if (errorMessage?.contains("Not authorized") == true) {
                      Result.Error(DataError.Network.INVALID_TOKEN)
                  } else {
                      Result.Error(DataError.Network.UNKNOWN)
                  }
                }
                403 -> {
                    if (errorMessage?.contains("emailNotVerified") == true)
                        Result.Error(DataError.Authentication.EMAIL_NOT_VERIFIED)
                    else if (errorMessage?.contains("accountBanned") == true)
                        Result.Error(DataError.Authentication.ACCOUNT_BANNED)
                    else
                        Result.Error(DataError.Network.UNKNOWN)
                }
                408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
                413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
                429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
                in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
                else -> Result.Error(DataError.Network.UNKNOWN)
            }
        }
        is SocketTimeoutException -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        is UnknownHostException -> Result.Error(DataError.Network.SERVER_ERROR)
        is IOException -> Result.Error(DataError.Network.NO_INTERNET)
        is SerializationException -> Result.Error(DataError.Network.SERIALIZATION)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}