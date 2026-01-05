package com.example.projektiop.util

import android.util.Patterns
import com.example.projektiop.util.ValidationError.Common
import com.example.projektiop.util.ValidationError.EmailError

fun emailValidator(email: String): List<ValidationError>{
    var out = emptyList<ValidationError>()
    val isEmail = Patterns.EMAIL_ADDRESS.matcher(email).matches()

    if(!isEmail) out += EmailError.NOT_EMAIL
    if(!email.isNotBlank()) out += Common.BLANK

    return out
}
