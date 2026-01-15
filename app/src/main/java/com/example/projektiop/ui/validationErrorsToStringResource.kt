package com.example.projektiop.ui

import com.example.projektiop.R
import com.example.projektiop.domain.ValidationError
import com.example.projektiop.domain.ValidationError.Common
import com.example.projektiop.domain.ValidationError.EmailError
import com.example.projektiop.domain.ValidationError.PasswordError

fun List<ValidationError>.mapToResource(): List<Int> {
    return this.map { item ->
        when (item) {
            Common.BLANK ->  R.string.error_field_empty
            EmailError.NOT_EMAIL -> R.string.error_invalid_email
            PasswordError.TOO_SHORT -> R.string.error_backup_password_too_short
            PasswordError.NO_UPPERCASE -> R.string.error_password_no_uppercase
            PasswordError.NO_DIGIT -> R.string.error_password_no_digit
            PasswordError.NO_LOWERCASE -> R.string.error_password_no_lowercase

            else -> R.string.error_unknown
        }
    }
}
