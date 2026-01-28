package com.example.projektiop.domain

import com.example.projektiop.domain.models.User

sealed interface InterestSimilarity {
    fun interestSimilarity(A: Set<String>, B: Set<String>): Double
}


