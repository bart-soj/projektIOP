package com.example.projektiop.util

import android.provider.DocumentsContract

sealed interface RootError

sealed interface DataError: RootError {
    enum class Network: DataError {
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERIALIZATION,
        UNKNOWN
    }
    enum class Local: DataError {
        DISK_FULL,
        DB_ERROR,
        NO_DATA
    }
    enum class Authentication: DataError {
        INVALID_EMAIL_PASSWORD,
        ACCOUNT_BANNED,
        EMAIL_NOT_VERIFIED
    }
}


enum class BackupError: RootError {
    WRONG_PASSWORD
}


sealed interface ValidationError: RootError {
    enum class Common: ValidationError {
        BLANK
    }

    enum class PasswordError: ValidationError {
        TOO_SHORT,
        NO_UPPERCASE,
        NO_DIGIT,
        NO_LOWERCASE
    }

    enum class EmailError: ValidationError {
        NOT_EMAIL
    }

    enum class UsernameError: ValidationError {
    }
}


