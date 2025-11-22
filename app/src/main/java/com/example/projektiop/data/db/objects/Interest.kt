package com.example.projektiop.data.db.objects

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Interest : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    var name: String = ""

    @Index
    var categoryId: ObjectId? = null // Reference to InterestCategory
    var category: InterestCategory? = null

    var description: String = ""
    var isArchived: Boolean = false

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    // use create method as constructor to ensure all necessary fields have values
    companion object {
        fun create(
            id: String,
            name: String,
            interestCategory: InterestCategory,
            categoryId: String,
            description: String = "",
            isArchived: Boolean? = false,
            createdAt: RealmInstant? = RealmInstant.now(),
            updatedAt: RealmInstant? = null
        ): Interest {
            return Interest().apply {
                this._id = ObjectId(id)
                this.name = name
                this.category = interestCategory
                this.categoryId = ObjectId(categoryId)
                this.description = description
                this.isArchived = isArchived == true
                this.createdAt = createdAt
                this.updatedAt = updatedAt
            }
        }
    }
}
