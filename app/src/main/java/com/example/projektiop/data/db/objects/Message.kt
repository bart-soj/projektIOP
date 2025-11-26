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
    var chatId: ObjectId = ObjectId()

    @Index
    var senderId: ObjectId = ObjectId()

    var content: String = ""

    var readBy: RealmList<ObjectId> = realmListOf()

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null

    companion object {
        fun create(
            id: String,
            chatId: String,
            senderId: String,
            content: String = "",
            readBy: List<String> = emptyList(),
            createdAt: RealmInstant? = RealmInstant.now(),
        ): Message {
            return Message().apply {
                this._id = ObjectId(id)
                this.chatId = ObjectId(chatId)
                this.senderId = ObjectId(senderId)
                this.content = content
                this.readBy = realmListOf(*readBy.map{ ObjectId(it) }.toTypedArray())
                this.createdAt = createdAt
                this.updatedAt = RealmInstant.now()
            }
        }
    }
}

