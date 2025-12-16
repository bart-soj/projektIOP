package com.example.projektiop.domain.models


data class UserInterest(
        val id: String,
        val userId: String,
        val interest: Interest,
        val customDescription: String = "",
)


data class Interest(
    val id: String,
    val name: String,
    val category: InterestCategory,
    val description: String
) {}


data class InterestCategory(
    val id: String,
    val name: String
) {}


data class SearchProfile(
    val name: String,
    val interests: List<Interest>
) {}