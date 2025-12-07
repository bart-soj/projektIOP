package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.FriendshipDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.objects.Friendship
import com.example.projektiop.data.db.objects.FriendshipStatus
import com.example.projektiop.data.db.objects.FriendshipType
import com.example.projektiop.data.repositories.DBRepository
import com.example.projektiop.data.repositories.FriendItem
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.util.mongoTimestampToRealmInstant
import com.example.projektiop.util.realmInstantToMongoTimestamp


private const val ID: String = "_id"


// FriendshipDto (API) -> Friendship (Realm)
fun FriendshipDto.toRealm(): Friendship {
    require(!this.friendshipId.isNullOrBlank()) { "Missing friendship id" }
    val id = this.friendshipId
    val user1Id = SharedPreferencesRepository.get(ID, "")
    require(user1Id.isNotEmpty()) { "Missing user1 id" }
    require(!this.user?._id.isNullOrBlank()) { "Missing user2 id" }
    val user2Id = this.user._id
    val requestedBy = this.requestedBy ?: user1Id

    val status = this.status
        ?.let { runCatching { FriendshipStatus.valueOf(it.uppercase()) }.getOrDefault(FriendshipStatus.NOT_FRIENDS) }
        ?: FriendshipStatus.PENDING

    val friendshipType = this.friendshipType
        ?.let { runCatching { FriendshipType.valueOf(it) }.getOrDefault(FriendshipType.UNVERIFIED) }
        ?: FriendshipType.UNVERIFIED

    return Friendship.create(
        id = id,
        user1Id = user1Id,
        user2Id = user2Id,
        requestedBy = requestedBy,
        status = status,
        friendshipType = friendshipType,
        blockedBy = this.blockedBy,
        isBlocked = this.isBlocked == true,
        createdAt = mongoTimestampToRealmInstant(this.createdAt),
        updatedAt = mongoTimestampToRealmInstant(this.updatedAt)
    )
}

fun Friendship.toDto(): FriendshipDto {
    return FriendshipDto(
        friendshipId = this._id.toString(),
        user = UserProfileResponse(_id = this.user2Id),
        status = this.status.name,
        friendshipType = this.friendshipType.name,
        requestedBy = this.requestedBy,
        isBlocked = this.isBlocked,
        blockedBy = this.blockedBy,
        createdAt = realmInstantToMongoTimestamp(this.createdAt),
        updatedAt = realmInstantToMongoTimestamp(this.updatedAt)
    )
}

fun FriendshipDto.toFriendItem(): FriendItem? {
    val userRef = this.user ?: return null
    val id = userRef._id ?: return null
    val displayName = userRef.profile?.displayName ?: userRef.username ?: "(bez nazwy)"
    val username = userRef.username ?: ""
    val avatarUrl = userRef.profile?.avatarUrl
    val friendshipId = this.friendshipId ?: return null
    val blockedBy = this.blockedBy

    return FriendItem(
        id = id,
        displayName = displayName,
        username = username,
        avatarUrl = avatarUrl,
        friendshipId = friendshipId,
        blockedBy = blockedBy
    )
}

fun Friendship.toFriendItem(): FriendItem {
    val user = DBRepository.getUserById(this.user2Id)
    val profile  = user?.profile
    return FriendItem(
        id = this.user2Id,
        displayName = user?.profile?.displayName.toString(),
        username = user?.username ?: "",
        avatarUrl = user?.profile?.avatarUrl,
        friendshipId = this._id.toString(),
        blockedBy = this.blockedBy
    )
}
