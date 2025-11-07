package com.example.projektiop.data.db.objects

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.util.UUID

class Chat : RealmObject {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()

    var participants: RealmList<String> = realmListOf() // References to User IDs

    var lastMessageId: String? = null // Reference to Message

    var lastMessageTimestamp: RealmInstant? = null

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    companion object {
        fun create(
            id: String,
            participants: RealmList<String>,
            lastMessageId: String,
            lastMessageTimestamp: RealmInstant? = RealmInstant.now(),
            createdAt: RealmInstant? = RealmInstant.now(),
            updatedAt: RealmInstant? = null
        ): Chat {
            require(participants.isNotEmpty()) {
                "Chat must have at least one participant"
            }

            return Chat().apply {
                this.id = id
                this.participants = participants
                this.lastMessageId = lastMessageId
                this.lastMessageTimestamp = lastMessageTimestamp
                this.createdAt = createdAt
                this.updatedAt = updatedAt
            }
        }
    }
}
