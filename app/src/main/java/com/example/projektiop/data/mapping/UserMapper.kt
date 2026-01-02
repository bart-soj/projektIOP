package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.ProfileDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.realm.objects.User
import com.example.projektiop.data.db.realm.objects.UserProfile
import com.example.projektiop.domain.models.Gender
import com.example.projektiop.domain.models.UserRole
import com.example.projektiop.util.mongoTimestampToRealmInstant
import com.example.projektiop.util.realmInstantToMongoTimestamp
import com.example.projektiop.util.toJavaInstant
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import java.time.Instant
import kotlin.String
import com.example.projektiop.domain.models.User as DomainUser
import com.example.projektiop.domain.models.User.UserProfile as DomainUserProfile


fun UserProfileResponse.toRealm(): User {
    require(!this._id.isNullOrBlank()) { "Missing user id" }
    val id = this._id
    require(!this.username.isNullOrBlank()) { "Missing username" }
    val username = this.username
    require(!this.email.isNullOrBlank()) { "Missing email" }
    val email = this.email

    val profile = UserProfile().apply {
        displayName = this@toRealm.profile?.displayName ?: this@toRealm.username
        avatarUrl = this@toRealm.profile?.avatarUrl.orEmpty()
        gender = this@toRealm.profile?.gender
            ?.takeIf { it.isNotBlank() }
            ?.uppercase()
            ?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
        birthDate = this.birthDate
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
            birthDate = this.profile?.birthDate,
            broadcastMessage = this.profile?.broadcastMessage,
            avatarUrl = this.profile?.avatarUrl
        ),
        _id = this._id.toHexString(),
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


fun User.toDomain(): DomainUser {
    return DomainUser(
        id = this._id.toHexString(),
        username = this.username,
        email = this.email,
        profile = DomainUserProfile(
            displayName = this.profile!!.displayName,
            avatarUrl = this.profile!!.avatarUrl,
            gender = this.profile!!.gender,
            birthDate = this.profile!!.birthDate,
            location = this.profile!!.location,
            bio = this.profile!!.bio,
            broadcastMessage = this.profile!!.broadcastMessage
        ),
        role = this.role!!,
        interests = this.interests.map { it.toDomain() },
        isBanned = this.isBanned,
        banReason = this.banReason,
        bannedAt = this.bannedAt?.toJavaInstant(),
        isDeleted = this.isDeleted,
        deletedAt = this.deletedAt?.toJavaInstant(),
        createdAt = this.createdAt?.toJavaInstant()
    )
}

