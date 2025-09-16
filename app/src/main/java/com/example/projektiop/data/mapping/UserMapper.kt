package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.Profile
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.objects.Gender
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.db.objects.UserProfile
import com.example.projektiop.data.db.objects.UserRole



fun UserProfileResponse.toRealm(): User {
    var user = User()
    user.id = this._id
    user.username = this.username
    user.email = this.email
    user.profile = UserProfile()
    user.profile?.displayName = this.profile?.displayName.toString()
    user.profile?.gender = Gender.valueOf(this.profile?.gender?.uppercase().toString())
    user.profile?.birthDate = mongoTimestampToRealmInstant(this.profile?.birthDate)
    user.profile?.location = this.profile?.location.toString()
    user.profile?.bio = this.profile?.bio.toString()
    user.profile?.broadcastMessage = this.profile?.broadcastMessage.toString()
    user.role = UserRole.valueOf(this.role?.uppercase().toString())
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
            gender = this.profile?.gender.toString(),
            location = this.profile?.location,
            birthDate = realmInstantToMongoTimestamp(this.profile?.birthDate),
            broadcastMessage = this.profile?.broadcastMessage
        ),
        _id = this.id,
        username = this.username,
        email = this.email,
        role = this.role.toString(),
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

