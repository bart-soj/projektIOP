package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.ProfileDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.objects.Gender
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.db.objects.UserProfile
import com.example.projektiop.data.db.objects.UserRole
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant


fun UserProfileResponse.toRealm(): User {
    require(!this._id.isNullOrBlank()) { "Missing user id" }
    val id = this._id
    require(!this.username.isNullOrBlank()) { "Missing username" }
    val username = this.username
    require(!this.email.isNullOrBlank()) { "Missing email" }
    val email = this.email

    val profile = UserProfile().apply {
        displayName = this@toRealm.profile?.displayName.orEmpty()
        avatarUrl = this@toRealm.profile?.avatarUrl.orEmpty()
        gender = this@toRealm.profile?.gender
            ?.takeIf { it.isNotBlank() }
            ?.uppercase()
            ?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
        birthDate = mongoTimestampToRealmInstant(this@toRealm.profile?.birthDate)
        location = this@toRealm.profile?.location.orEmpty()
        bio = this@toRealm.profile?.bio.orEmpty()
        broadcastMessage = this@toRealm.profile?.broadcastMessage.orEmpty()
    }

    val roleEnum = this.role
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
        ?.let { runCatching { UserRole.valueOf(it) }.getOrDefault(UserRole.USER) }
        ?: UserRole.USER

    val interestIds = realmListOf<String>().apply {
        addAll(this@toRealm.interests?.mapNotNull { it.userInterestId } ?: emptyList())
    }

    return User.create(
        id = id,
        username = username,
        email = email,
        profile = profile,
        role = roleEnum,
        interestIds = interestIds,
        isBanned = this.isBanned ?: false,
        banReason = this.banReason,
        bannedAt = mongoTimestampToRealmInstant(this.bannedAt),
        isTestAccount = this.isTestAccount ?: false,
        isEmailVerified = this.isEmailVerified ?: false,
        isDeleted = this.isDeleted ?: false,
        deletedAt = mongoTimestampToRealmInstant(this.deletedAt),
        createdAt = mongoTimestampToRealmInstant(this.createdAt),
        updatedAt = RealmInstant.now()
    )
}



fun User.toUserProfileResponse(): UserProfileResponse {
    return UserProfileResponse(
        profile = ProfileDto(
            displayName = this.profile?.displayName,
            bio = this.profile?.bio,
            gender = this.profile?.gender?.name,
            location = this.profile?.location,
            birthDate = realmInstantToMongoTimestamp(this.profile?.birthDate),
            broadcastMessage = this.profile?.broadcastMessage,
            avatarUrl = this.profile?.avatarUrl
        ),
        _id = this._id.toString(),
        username = this.username,
        email = this.email,
        role = this.role?.name,
        isBanned = this.isBanned,
        banReason = this.banReason,
        bannedAt = realmInstantToMongoTimestamp(this.bannedAt),
        isTestAccount = this.isTestAccount,
        isEmailVerified = this.isEmailVerified,
        isDeleted = this.isDeleted,
        deletedAt = realmInstantToMongoTimestamp(this.deletedAt),
        createdAt = realmInstantToMongoTimestamp(this.createdAt),
        updatedAt = realmInstantToMongoTimestamp(this.updatedAt),
        __v = null,
        interests = null // TODO() need to store in db first
    )
}

