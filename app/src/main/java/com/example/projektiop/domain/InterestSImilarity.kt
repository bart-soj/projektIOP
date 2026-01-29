package com.example.projektiop.domain

import com.example.projektiop.domain.models.User

sealed interface InterestSimilarity {
    fun <T> interestSimilarity(A: Set<T>, B: Set<T>): Double
}


