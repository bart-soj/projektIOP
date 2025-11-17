package com.example.projektiop.data.db.objects

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.annotations.Ignore
import java.util.UUID

enum class UserRole {
    USER,
    PREMIUM_USER
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}

class UserProfile : EmbeddedRealmObject {
    var displayName: String = ""
    var avatarUrl: String = ""

    @Ignore
    var gender: Gender?
        get() = _gender?.let { Gender.valueOf(it) }
        set(value) { _gender = value?.name }

    private var _gender: String? = null

    var birthDate: RealmInstant? = null
    var location: String = ""
    var bio: String = ""
    var broadcastMessage: String = ""
 }

class User : RealmObject {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()

    @Index
    var username: String = ""

    @Index
    var email: String = ""

    var profile: UserProfile? = null

    @Ignore
    var role: UserRole?
        get() = UserRole.valueOf(_role)
        set(value) { _role = (value?.name ?: "USER") }

    private var _role: String = UserRole.USER.name

    var interests: RealmList<String> = realmListOf() // list of ids of user interests

    var isBanned: Boolean = false
    var banReason: String? = null
    var bannedAt: RealmInstant? = null
    var isTestAccount: Boolean = false
    var isEmailVerified: Boolean = false


    @Index
    var isDeleted: Boolean = false
    var deletedAt: RealmInstant? = null

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    // use create method as constructor to ensure all necessary fields have values
    companion object {
        fun create(
            id: String,
            username: String,
            email: String,
            profile: UserProfile? = null,
            role: UserRole = UserRole.USER,
            interests: RealmList<String> = realmListOf(),
            isBanned: Boolean = false,
            banReason: String? = null,
            bannedAt: RealmInstant? = null,
            isTestAccount: Boolean = false,
            isEmailVerified: Boolean = false,
            isDeleted: Boolean = false,
            deletedAt: RealmInstant? = null,
            createdAt: RealmInstant? = RealmInstant.now(),
            updatedAt: RealmInstant? = null
        ): User {
            return User().apply {
                this.id = id
                this.username = username
                this.email = email
                this.profile = profile
                this.role = role
                this.interests = interests
                this.isBanned = isBanned
                this.banReason = banReason
                this.bannedAt = bannedAt
                this.isTestAccount = isTestAccount
                this.isEmailVerified = isEmailVerified
                this.isDeleted = isDeleted
                this.deletedAt = deletedAt
                this.createdAt = createdAt
                this.updatedAt = updatedAt
            }
        }
    }
}

