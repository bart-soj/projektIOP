package com.example.projektiop.domain

sealed interface InterestSimilarity {
    fun <T> interestSimilarity(A: Set<T>, B: Set<T>): Double
}


