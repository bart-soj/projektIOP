package com.example.projektiop.domain

import kotlin.math.sqrt

class OtsukaOchiaiSimilarityImpl() : InterestSimilarity {
    override fun <T> interestSimilarity(a: Set<T>, b: Set<T>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val intersectionSize = a.intersect(b).size
        return intersectionSize / sqrt(a.size.toDouble() * b.size.toDouble())
    }
}