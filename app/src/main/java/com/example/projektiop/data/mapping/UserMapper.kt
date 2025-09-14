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
    var user = User()
    user.id = this._id
    user.username = this.username
    user.email = this.email
    user.profile = UserProfile()
    user.profile?.displayName = this.profile?.displayName.toString()
    user.profile?.gender = Gender.valueOf(this.profile?.gender?.uppercase().toString())
    user.profile?.birthDate = null // TODO() add a function to map between realm instant and mongodb timestamp
    user.profile?.location = this.profile?.location.toString()
    user.profile?.bio = this.profile?.bio.toString()
    user.profile?.broadcastMessage = this.profile?.broadcastMessage.toString()

    return user
}


fun User.toUserProfileResponse(): UserProfileResponse {
    return UserProfileResponse(
        profile = Profile(
            displayName = this.profile?.displayName,
            bio = this.profile?.bio,
            gender = this.profile?.gender.toString(),
            location = this.profile?.location,
            birthDate = this.profile?.birthDate.toString(),
            broadcastMessage = this.profile?.broadcastMessage
        ),
        _id = this.id,
        username = this.username,
        email = this.email,
        role = this.role.toString(),
        isBanned = this.isBanned,
        banReason = this.banReason,
        bannedAt = this.bannedAt.toString(),
        isTestAccount = this.isTestAccount,
        isEmailVerified = this.isEmailVerified,
        isDeleted = this.isDeleted,
        deletedAt = this.deletedAt.toString(),
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString(),
        __v = null,
        interests = null
    )
}

