package com.example.projektiop.domain.models

import java.time.Instant


enum class UserRole {
    USER,
    PREMIUM_USER
}


enum class Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}


data class User(
    val id: String,
    val username: String,
    val email: String,
    val profile: UserProfile,
    val role: UserRole = UserRole.USER,

    val interests: List<UserInterest> = emptyList(),

    val isBanned: Boolean = false,
    val banReason: String? = null,
    val bannedAt: Instant? = null,

    val isDeleted: Boolean = false,
    val deletedAt: Instant? = null,

    val createdAt: Instant? = null
) {
    class UserProfile(
        val displayName: String = "",
        val avatarUrl: String = "",
        val gender: Gender? = null,
        val birthDate: String? = null,
        val location: String = "",
        val bio: String = "",
        val broadcastMessage: String = ""
    )
}