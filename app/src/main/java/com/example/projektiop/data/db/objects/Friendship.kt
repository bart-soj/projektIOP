package com.example.projektiop.data.db.objects

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.RealmInstant
import java.util.UUID

enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    BLOCKED
}

enum class FriendshipType {
    UNVERIFIED,
    VERIFIED
}

class Friendship : RealmObject {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()

    @Index
    var user1: String = "" // Reference to User

    @Index
    var user2: String = "" // Reference to User

    @Ignore
    var status: FriendshipStatus
        get() {
            return FriendshipStatus.valueOf(_status)
        }
        set(value) {
            _status = value.name
        }

    private var _status: String = FriendshipStatus.PENDING.name

    var requestedBy: String = "" // Reference to User

    @Ignore
    var friendshipType: FriendshipType
        get() {
            return FriendshipType.valueOf(_friendshipType)
        }
        set(value) {
            _friendshipType = value.name
        }

    private var _friendshipType: String = FriendshipType.UNVERIFIED.name

    var blockedBy: String? = null // Reference to User

    var isBlocked: Boolean = false

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null
}


