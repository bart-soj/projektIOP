package com.example.projektiop.data.db.realm.objects

import com.example.projektiop.domain.models.base64
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Chat : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    var participants: RealmList<String> = realmListOf()

    var lastMessageId: String? = null

    var lastMessageTimestamp: RealmInstant? = null

    var chatKey: base64? = null

    var lostHistory: Boolean = false

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    companion object {
        fun create(
            id: String,
            participants: List<String>,
            chatKey: base64,
            lastMessageId: String?,
            lastMessageTimestamp: RealmInstant? = null,
            lostHistory: Boolean? = false,
            createdAt: RealmInstant? = null,
        ): Chat {
            require(participants.isNotEmpty()) {
                "Chat must have at least one participant"
            }

            return Chat().apply {
                this._id = ObjectId(id)
                this.participants = realmListOf(*participants.toTypedArray())
                this.lastMessageId = lastMessageId
                this.chatKey = chatKey
                this.lastMessageTimestamp = lastMessageTimestamp
                this.lostHistory = lostHistory == true
                this.createdAt = createdAt
                this.updatedAt = RealmInstant.now()
            }
        }
    }
}
