package com.example.projektiop.domain.models


data class UserInterest(
        val userId: String,
        val interest: Interest,
        val customDescription: String = "",
)


data class Interest(
    val name: String,
    val category: InterestCategory,
    val description: String
) {}


data class InterestCategory(
    val name: String
) {}