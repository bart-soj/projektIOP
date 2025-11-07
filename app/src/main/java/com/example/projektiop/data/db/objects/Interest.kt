package com.example.projektiop.data.db.objects

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import java.util.UUID

class Interest : RealmObject {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()

    @Index
    var name: String = ""

    @Index
    var categoryId: String? = null // Reference to InterestCategory

    var description: String = ""
    var isArchived: Boolean = false

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    // use create method as constructor to ensure all necessary fields have values
    companion object {
        fun create(
            id: String,
            name: String,
            categoryId: String? = null,
            description: String = "",
            isArchived: Boolean = false,
            createdAt: RealmInstant? = RealmInstant.now(),
            updatedAt: RealmInstant? = null
        ): Interest {
            return Interest().apply {
                this.id = id
                this.name = name
                this.categoryId = categoryId
                this.description = description
                this.isArchived = isArchived
                this.createdAt = createdAt
                this.updatedAt = updatedAt
            }
        }
    }
}
