package com.example.projektiop.data.db.objects

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.RealmInstant
import java.util.UUID

class UserInterest : RealmObject {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()

    @Index
    var userId: String = "" // Reference to User

    @Index
    var interestId: String = "" // Reference to Interest

    var customDescription: String = ""

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null
}
