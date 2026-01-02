package com.example.projektiop.util

import com.example.projektiop.util.ValidationError.Common
import com.example.projektiop.util.ValidationError.EmailError
import com.example.projektiop.util.ValidationError.PasswordError

fun List<ValidationError>.mapToResource(): List<Int> {
    return this.map { item ->
        when (item) {
            Common.BLANK ->  com.example.projektiop.R.string.error_field_empty
            EmailError.NOT_EMAIL -> com.example.projektiop.R.string.error_invalid_email
            PasswordError.TOO_SHORT -> com.example.projektiop.R.string.error_backup_password_too_short
            PasswordError.NO_UPPERCASE -> com.example.projektiop.R.string.error_password_no_uppercase
            PasswordError.NO_DIGIT -> com.example.projektiop.R.string.error_password_no_digit
            PasswordError.NO_LOWERCASE -> com.example.projektiop.R.string.error_password_no_lowercase

            else -> com.example.projektiop.R.string.error_unknown
        }
    }
}
