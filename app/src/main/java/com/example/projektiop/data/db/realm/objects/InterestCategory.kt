package com.example.projektiop.data.db.realm.objects

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class InterestCategory : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    var name: String = ""

    // var description: String = ""

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    // use create method as constructor to ensure all necessary fields have values
    companion object {
        fun create(
            id: String,
            name: String,
            // description: String = "",
            createdAt: RealmInstant? = RealmInstant.now(),
            updatedAt: RealmInstant? = null
        ): InterestCategory {
            return InterestCategory().apply {
                this._id = ObjectId(id)
                this.name = name
                // this.description = description
                this.createdAt = createdAt
                this.updatedAt = RealmInstant.now()
            }
        }
    }
}