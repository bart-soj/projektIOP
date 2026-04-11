package com.example.projektiop.data.api

import okhttp3.ResponseBody
import retrofit2.Converter

class ErrorConverter(
    private val retrofit: retrofit2.Retrofit
) {
    fun <T> convert(type: Class<T>): Converter<ResponseBody, T> {
        return retrofit.responseBodyConverter(type, emptyArray())
    }
}