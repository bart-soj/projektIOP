package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.Profile
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.api.UserStats
import com.example.projektiop.data.db.objects.Gender
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.db.objects.UserProfile
import com.google.gson.annotations.SerializedName
import io.realm.kotlin.types.RealmInstant
import java.time.Instant
import kotlin.String

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
    user.profile?.displayName = this.profile?.displayName ?: ""
    user.profile?.avatarUrl = this.profile?.avatarUrl ?: user.profile?.avatarUrl ?: ""
    // Safe gender mapping
    val genderEnum = this.profile?.gender
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
        ?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
    user.profile?.gender = genderEnum
    user.profile?.birthDate = null // TODO: map between RealmInstant and MongoDB timestamp if needed
    user.profile?.location = this.profile?.location ?: ""
    user.profile?.bio = this.profile?.bio ?: ""
    user.profile?.broadcastMessage = this.profile?.broadcastMessage ?: ""

    return user
}


fun User.toUserProfileResponse(): UserProfileResponse {
    return UserProfileResponse(
        profile = Profile(
            displayName = this.profile?.displayName,
            bio = this.profile?.bio,
            gender = this.profile?.gender?.name,
            location = this.profile?.location,
            birthDate = this.profile?.birthDate?.toString(),
            broadcastMessage = this.profile?.broadcastMessage,
            avatarUrl = this.profile?.avatarUrl
        ),
        _id = this.id,
        username = this.username,
        email = this.email,
        role = this.role.name,
        isBanned = this.isBanned,
        banReason = this.banReason,
        bannedAt = this.bannedAt?.toString(),
        isTestAccount = this.isTestAccount,
        isEmailVerified = this.isEmailVerified,
        isDeleted = this.isDeleted,
        deletedAt = this.deletedAt?.toString(),
        createdAt = this.createdAt?.toString(),
        updatedAt = this.updatedAt?.toString(),
        __v = null,
        interests = null
    )
}

