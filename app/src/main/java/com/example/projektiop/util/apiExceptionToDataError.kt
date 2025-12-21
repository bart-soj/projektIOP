package com.example.projektiop.util


import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException
import retrofit2.HttpException
import kotlinx.serialization.SerializationException

fun <T> apiExceptionToDataError(e: Exception): Result.Error<T, DataError> {
    return when (e) {
        is HttpException -> when (e.code()) {
            401 -> Result.Error(DataError.Authentication.INVALID_EMAIL_PASSWORD)
            403 -> if (e.message?.contains("Please verify your email address before logging in.") == true)
                Result.Error(DataError.Authentication.EMAIL_NOT_VERIFIED)
            else if (e.message?.contains("Your account has been banned.") == true)
                Result.Error(DataError.Authentication.ACCOUNT_BANNED)
            else
                Result.Error(DataError.Network.UNKNOWN)
            408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
            413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
            429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
            in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
            else -> Result.Error(DataError.Network.UNKNOWN)
        }
        is SocketTimeoutException -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        is UnknownHostException -> Result.Error(DataError.Network.SERVER_ERROR)
        is IOException -> Result.Error(DataError.Network.NO_INTERNET)
        is SerializationException -> Result.Error(DataError.Network.SERIALIZATION)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}