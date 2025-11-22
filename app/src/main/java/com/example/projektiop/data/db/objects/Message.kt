package com.example.projektiop.data.db.objects

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.RealmInstant
import org.mongodb.kbson.ObjectId

class Message : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    var chatId: String = ""

    @Index
    var senderId: String = ""

    var content: String = ""

    var readBy: RealmList<String> = realmListOf()

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    companion object {
        fun create(
            id: String,
            chatId: String,
            senderId: String,
            content: String = "",
            readBy: RealmList<String> = realmListOf(),
            createdAt: RealmInstant? = RealmInstant.now(),
            updatedAt: RealmInstant? = null
        ): Message {
            return Message().apply {
                this._id = ObjectId(id)
                this.chatId = chatId
                this.senderId = senderId
                this.content = content
                this.readBy = readBy
                this.createdAt = createdAt
                this.updatedAt = updatedAt
            }
        }
    }
}

