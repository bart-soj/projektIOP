package com.example.projektiop.domain

import com.example.projektiop.domain.models.User

sealed interface UserSimilarity{
    fun userSimilarity(A: User, B: User): Double
}