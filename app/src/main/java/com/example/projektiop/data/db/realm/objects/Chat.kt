package com.example.projektiop.data.db.realm.objects

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Chat : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    var participants: RealmList<String> = realmListOf() // References to User IDs

    var lastMessageId: String? = null // Reference to Message

    var lastMessageTimestamp: RealmInstant? = null

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    companion object {
        fun create(
            id: String,
            participants: List<String>,
            lastMessageId: String,
            lastMessageTimestamp: RealmInstant? = RealmInstant.now(),
            createdAt: RealmInstant? = null,
        ): Chat {
            require(participants.isNotEmpty()) {
                "Chat must have at least one participant"
            }

            return Chat().apply {
                this._id = ObjectId(id)
                this.participants = realmListOf(*participants.toTypedArray())
                this.lastMessageId = lastMessageId
                this.lastMessageTimestamp = lastMessageTimestamp
                this.createdAt = createdAt
                this.updatedAt = RealmInstant.now()
            }
        }
    }
}
