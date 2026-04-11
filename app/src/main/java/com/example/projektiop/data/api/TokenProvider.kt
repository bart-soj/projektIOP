package com.example.projektiop.data.api

interface TokenProvider {
    fun getToken(): String?
}