package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.FriendshipDto
import com.example.projektiop.data.api.UserRef
import com.example.projektiop.data.api.ProfileRef
import com.example.projektiop.data.db.objects.Friendship
import com.example.projektiop.data.db.objects.FriendshipStatus
import com.example.projektiop.data.db.objects.FriendshipType
import com.example.projektiop.data.repositories.DBRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import io.realm.kotlin.types.RealmInstant
import java.util.UUID


private const val ID: String = "_id"


// FriendshipDto (API) → Friendship (Realm)
fun FriendshipDto.toRealm(): Friendship {
    val friendship = Friendship()
    friendship.id = this.friendshipId ?: this._id ?: UUID.randomUUID().toString() // new UUID creation here is hacky

    friendship.user1 = SharedPreferencesRepository.get(ID, "")
    friendship.user2 = this.user?._id ?: ""

    friendship.status = this.status
        ?.let { runCatching { FriendshipStatus.valueOf(it) }.getOrDefault(FriendshipStatus.PENDING) }
        ?: FriendshipStatus.PENDING

    friendship.friendshipType = this.friendshipType
        ?.let { runCatching { FriendshipType.valueOf(it) }.getOrDefault(FriendshipType.UNVERIFIED) }
        ?: FriendshipType.UNVERIFIED

    friendship.isBlocked = this.isBlocked ?: false
    friendship.blockedBy = this.blockedBy
    friendship.requestedBy = this.requestedByUsername.toString()
    friendship.createdAt = mongoTimestampToRealmInstant(this.createdAt)
    friendship.updatedAt = mongoTimestampToRealmInstant(this.updatedAt)

    return friendship
}