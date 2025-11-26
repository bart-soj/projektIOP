package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.MessageDto
import com.example.projektiop.data.db.objects.Message
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import com.google.gson.JsonElement
import io.realm.kotlin.ext.realmListOf

fun MessageDto.toRealm(): Message {
    require(!this._id.isNullOrBlank()) { "Missing message id" }
    val id = this._id
    val chatId = this.chatId
    require(!chatId.isNullOrBlank()) { "Missing chat id" }
    require(!(this.senderId).isNullOrBlank()) { "Missing sender id" }
    val senderId = this.senderId

    val readByList = realmListOf<String>().apply {
        addAll(this@toRealm.readBy?.mapNotNull { it } ?: emptyList())
    }
    val createdAt = mongoTimestampToRealmInstant(this.createdAt)
    val updatedAt = mongoTimestampToRealmInstant(this.updatedAt)

    return Message.create(
        id = id,
        chatId = chatId,
        senderId = senderId,
        content = this.content ?: "",
        readBy = readByList,
        createdAt = createdAt,
    )
}
