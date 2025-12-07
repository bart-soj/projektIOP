package com.example.projektiop.util

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
        DB_ERROR
    }
}

sealed interface ValidationError: RootError {
    enum class Common: ValidationError {
        BLANK
    }

    enum class PasswordError: ValidationError {
        TOO_SHORT,
        NO_UPPERCASE,
        NO_DIGIT,
        NO_LOWERCASE,
    }
    enum class EmailError: ValidationError {
        NOT_EMAIL,
    }
    enum class UsernameError: ValidationError {
    }
}


