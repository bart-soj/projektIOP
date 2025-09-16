package com.example.projektiop.data.db.objects

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.RealmInstant
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
}

