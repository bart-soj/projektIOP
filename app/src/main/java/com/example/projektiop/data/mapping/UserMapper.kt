package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.Profile
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.api.UserStats
import com.example.projektiop.data.db.objects.Gender
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.db.objects.UserProfile
import com.google.gson.annotations.SerializedName
import io.realm.kotlin.types.RealmInstant
import kotlin.String

fun UserProfileResponse.toRealm(): User {
    var user = User()
    user.id = this._id
    user.username = this.username
    user.email = this.email
    user.profile = UserProfile()
    user.profile?.displayName = this.profile.displayName.toString()
    user.profile?.gender = this.profile.gender as Gender?
    user.profile?.birthDate = this.profile.birthDate as RealmInstant?
    user.profile?.location = this.profile.location.toString()
    user.profile?.bio = this.profile.bio.toString()
    user.profile?.broadcastMessage = this.profile.broadcastMessage.toString()

    return user
}

