package com.example.projektiop.util

import com.example.projektiop.util.ValidationError.PasswordError

fun backupPasswordValidator(password: String): List<ValidationError> {
    var out = emptyList<ValidationError>()
    val hasMinimumLength = password.length >= 12
    val hasLowercase = password.any { it.isLowerCase() }
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }

    if (!hasMinimumLength) out += PasswordError.TOO_SHORT
    if (!hasLowercase) out += PasswordError.NO_LOWERCASE
    if (!hasUppercase) out += PasswordError.NO_UPPERCASE
    if (!hasDigit) out += PasswordError.NO_DIGIT

    return out
}