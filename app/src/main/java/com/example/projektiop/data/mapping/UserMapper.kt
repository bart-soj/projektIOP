package com.example.projektiop.data.mapping

import androidx.compose.ui.semantics.Role
import com.example.projektiop.data.api.Profile
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.objects.Gender
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.db.objects.UserProfile
import com.example.projektiop.data.db.objects.UserRole


fun UserProfileResponse.toRealm(): User {
    val user = User()
    // Primary key – must not be null/blank; caller should ensure presence, otherwise DB save should be skipped
    val idFromApi = this._id
    if (idFromApi != null) {
        user.id = idFromApi
    }
    this.username?.let { user.username = it }
    this.email?.let { user.email = it }
    user.profile = UserProfile()
    user.profile?.displayName = this.profile?.displayName.toString()
    // Safe gender mapping
    val genderEnum = this.profile?.gender
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
        ?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
    user.profile?.gender = genderEnum
    user.profile?.birthDate = mongoTimestampToRealmInstant(this.profile?.birthDate)
    user.profile?.location = this.profile?.location.toString()
    user.profile?.bio = this.profile?.bio.toString()
    user.profile?.broadcastMessage = this.profile?.broadcastMessage.toString()
    user.profile?.avatarUrl = this.profile?.avatarUrl ?: ""
    // Safe role mapping
    val roleEnum: UserRole? = this.role
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
        ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
    user.role = roleEnum
    user.isBanned = this.isBanned ?: true
    user.banReason = this.banReason
    user.bannedAt = mongoTimestampToRealmInstant(this.bannedAt)
    user.isTestAccount = this.isTestAccount ?: false
    user.isEmailVerified = this.isEmailVerified ?: false
    user.isDeleted = this.isDeleted ?: false
    user.deletedAt = mongoTimestampToRealmInstant(this.deletedAt)
    user.createdAt = mongoTimestampToRealmInstant(this.createdAt)
    user.updatedAt = mongoTimestampToRealmInstant(this.updatedAt)

    return user
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

