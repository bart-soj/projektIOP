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
    val id = this.friendshipId ?: this._id ?: return throw IllegalArgumentException("Missing friendship id")
    val user1 = SharedPreferencesRepository.get(ID, "")
    val user2 = this.user?._id ?: return throw IllegalArgumentException("Missing user2 id")
    val requestedBy = this.requestedByUsername ?: user1

    val status = this.status
        ?.let { runCatching { FriendshipStatus.valueOf(it) }.getOrDefault(FriendshipStatus.PENDING) }
        ?: FriendshipStatus.PENDING

    val friendshipType = this.friendshipType
        ?.let { runCatching { FriendshipType.valueOf(it) }.getOrDefault(FriendshipType.UNVERIFIED) }
        ?: FriendshipType.UNVERIFIED

    return Friendship.create(
        id = id,
        user1 = user1,
        user2 = user2,
        requestedBy = requestedBy,
        status = status,
        friendshipType = friendshipType,
        blockedBy = this.blockedBy,
        isBlocked = this.isBlocked == true,
        createdAt = mongoTimestampToRealmInstant(this.createdAt),
        updatedAt = mongoTimestampToRealmInstant(this.updatedAt)
    )

}
