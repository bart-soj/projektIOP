package com.example.projektiop.data.db.realm.objects

import io.realm.kotlin.ext.backlinks
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.RealmInstant
import org.mongodb.kbson.ObjectId

class UserInterest : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    var userId: ObjectId? = null // Reference to User
    val user: RealmResults<User> by backlinks(User::interests)

    @Index
    var interestId: ObjectId? = null// Reference to Interest
    var interest: Interest? = null

    var customDescription: String = ""

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    // use create method as constructor to ensure all necessary fields have values
    companion object {
        fun create(
            id: String,
            userId: String,
            interestId: String,
            interest: Interest,
            customDescription: String = "",
            createdAt: RealmInstant? = null,
        ): UserInterest {
            return UserInterest().apply {
                this._id = ObjectId(id)
                this.userId = ObjectId(userId)
                this.interestId = ObjectId(interestId)
                this.interest = interest
                this.customDescription = customDescription
                this.createdAt = createdAt
                this.updatedAt = RealmInstant.now()
            }
        }
    }
}


