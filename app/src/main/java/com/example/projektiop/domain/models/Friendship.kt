package com.example.projektiop.domain.models

import java.time.Instant


enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    BLOCKED,
    NOT_FRIENDS
}


enum class FriendshipType {
    UNVERIFIED,
    VERIFIED
}


class Friendship(
    val id: String,
    val friend: User,
    val status: FriendshipStatus,
    val type: FriendshipType = FriendshipType.UNVERIFIED,
    val requestedBy: String,
    val blockedBy: String?,
    val isBlocked: Boolean,
    val createdAt: Instant
) {}