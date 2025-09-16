package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.MessageDto
import com.example.projektiop.data.db.objects.Message
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList

fun MessageDto.toRealm(): Message {
    var message = Message()

    message.id = this._id.toString()
    message.chatId = this.chatId.toString()
    message.senderId = this.senderId.toString()
    message.content = this.content.toString()
    message.readBy = (this.readBy?.mapNotNull{ it._id } ?: emptyList<String>()) as RealmList<String>
    message.createdAt = mongoTimestampToRealmInstant(this.createdAt)

    return message
}