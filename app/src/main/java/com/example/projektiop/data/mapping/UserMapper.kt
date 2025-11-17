package com.example.projektiop.data.mapping

import androidx.compose.ui.semantics.Role
import com.example.projektiop.data.api.Profile
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.objects.Gender
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.db.objects.UserProfile
import com.example.projektiop.data.db.objects.UserRole
import io.realm.kotlin.ext.realmListOf


fun UserProfileResponse.toRealm(): User {
    val id = this._id ?: throw IllegalArgumentException("Missing user id")
    val username = this.username ?: throw IllegalArgumentException("Missing username")
    val email = this.email ?: throw IllegalArgumentException("Missing email")

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

    val interests = realmListOf<String>().apply {
        addAll(this@toRealm.interests?.mapNotNull { it.userInterestId } ?: emptyList())
    }

    return User.create(
        id = id,
        username = username,
        email = email,
        profile = profile,
        role = roleEnum,
        interests = interests,
        isBanned = this.isBanned ?: false,
        banReason = this.banReason,
        bannedAt = mongoTimestampToRealmInstant(this.bannedAt),
        isTestAccount = this.isTestAccount ?: false,
        isEmailVerified = this.isEmailVerified ?: false,
        isDeleted = this.isDeleted ?: false,
        deletedAt = mongoTimestampToRealmInstant(this.deletedAt),
        createdAt = mongoTimestampToRealmInstant(this.createdAt),
        updatedAt = mongoTimestampToRealmInstant(this.updatedAt)
    )
}



fun User.toUserProfileResponse(): UserProfileResponse {
    return UserProfileResponse(
        profile = Profile(
            displayName = this.profile?.displayName,
            bio = this.profile?.bio,
            gender = this.profile?.gender?.name,
            location = this.profile?.location,
            birthDate = realmInstantToMongoTimestamp(this.profile?.birthDate),
            broadcastMessage = this.profile?.broadcastMessage,
            avatarUrl = this.profile?.avatarUrl
        ),
        _id = this.id,
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

