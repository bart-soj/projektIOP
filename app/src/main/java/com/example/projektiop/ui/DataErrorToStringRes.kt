package com.example.projektiop.ui

import androidx.annotation.StringRes
import com.example.projektiop.R
import com.example.projektiop.domain.DataError
import com.example.projektiop.domain.DataError.Authentication
import com.example.projektiop.domain.DataError.Local
import com.example.projektiop.domain.DataError.Network

@StringRes
fun DataError.toStringRes(): Int = when (this) {
    Authentication.INVALID_EMAIL_PASSWORD -> R.string.error_invalid_email_password
    Authentication.ACCOUNT_BANNED -> R.string.error_account_banned
    Authentication.EMAIL_NOT_VERIFIED -> R.string.error_email_not_verified
    Authentication.EMAIL_USERNAME_TAKEN -> R.string.error_email_username_taken
    Local.DISK_FULL -> R.string.error_disk_full
    Local.DB_ERROR -> R.string.error_db_error
    Local.NO_DATA -> R.string.error_no_data
    Network.REQUEST_TIMEOUT -> R.string.error_request_timeout
    Network.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
    Network.NO_INTERNET -> R.string.error_no_internet
    Network.PAYLOAD_TOO_LARGE -> R.string.error_payload_too_large
    Network.SERVER_ERROR -> R.string.error_server_error
    Network.SERIALIZATION -> R.string.error_serialization
    Network.INVALID_TOKEN -> R.string.error_invalid_token
    Network.UNKNOWN -> R.string.error_unknown
}
