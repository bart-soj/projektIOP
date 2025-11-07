package com.example.projektiop.data.db.objects

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey

class InterestCategory {
    class InterestCategory : RealmObject {
        @PrimaryKey
        var id: String = ""

        @Index
        var name: String = ""

        var description: String = ""

        var createdAt: RealmInstant? = null
        var updatedAt: RealmInstant? = null

        // use create method as constructor to ensure all necessary fields have values
        companion object {
            fun create(
                id: String,
                name: String,
                description: String = "",
                createdAt: RealmInstant? = RealmInstant.now(),
                updatedAt: RealmInstant? = null
            ): InterestCategory {
                return InterestCategory().apply {
                    this.id = id
                    this.name = name
                    this.description = description
                    this.createdAt = createdAt
                    this.updatedAt = updatedAt
                }
            }
        }
    }
}