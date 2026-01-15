package com.example.projektiop.util

import android.util.Patterns
import com.example.projektiop.domain.ValidationError
import com.example.projektiop.domain.ValidationError.Common
import com.example.projektiop.domain.ValidationError.EmailError

fun emailValidator(email: String): List<ValidationError>{
    var out = emptyList<ValidationError>()
    val isEmail = Patterns.EMAIL_ADDRESS.matcher(email).matches()

    if(!isEmail) out += EmailError.NOT_EMAIL
    if(!email.isNotBlank()) out += Common.BLANK

    return out
}
