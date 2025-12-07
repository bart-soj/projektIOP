package com.example.projektiop.data.db.objects

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.RealmInstant
import org.mongodb.kbson.ObjectId


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


class Friendship() : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    var user1Id: String = ""
    // var user1: User? = null

    @Index
    var user2Id: String = ""
    // var user2: User? = null

    @Ignore
    var status: FriendshipStatus
        get() {
            return FriendshipStatus.valueOf(_status)
        }
        set(value) {
            _status = value.name
        }
    private var _status: String = FriendshipStatus.NOT_FRIENDS.name

    var requestedBy: String = ""

    @Ignore
    var friendshipType: FriendshipType
        get() {
            return FriendshipType.valueOf(_friendshipType)
        }
        set(value) {
            _friendshipType = value.name
        }
    private var _friendshipType: String = FriendshipType.UNVERIFIED.name

    var blockedBy: String? = null
    var isBlocked: Boolean = false

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    // use create method as constructor to ensure all necessary fields have values
    companion object {
        fun create (
            id: String,
            user1Id: String,
            user2Id: String,
            // user1: User,
            // user2: User,
            requestedBy: String,
            status: FriendshipStatus = FriendshipStatus.PENDING,
            friendshipType: FriendshipType = FriendshipType.UNVERIFIED,
            blockedBy: String? = null,
            isBlocked: Boolean = false,
            createdAt: RealmInstant? = RealmInstant.now(),
            updatedAt: RealmInstant? = null
        ) : Friendship {
            return Friendship().apply {
                this._id = ObjectId(id)
                this.user1Id = user1Id
                this.user2Id = user2Id
                // this.user1 = user1
                // this.user2 = user2
                this.requestedBy = requestedBy
                this.status = status
                this.friendshipType = friendshipType
                this.blockedBy = blockedBy
                this.isBlocked = isBlocked
                this.createdAt = createdAt
                this.updatedAt = RealmInstant.now()
            }
        }
    }
}
