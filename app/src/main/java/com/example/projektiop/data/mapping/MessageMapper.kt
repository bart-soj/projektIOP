package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.MessageDto
import com.example.projektiop.data.db.realm.objects.Message
import com.example.projektiop.data.util.mongoTimestampToRealmInstant
import com.example.projektiop.data.util.toJavaInstant
import io.realm.kotlin.ext.realmListOf
import com.example.projektiop.domain.models.Message as DomainMessage

fun MessageDto.toRealm(decryptedContent: String): Message {
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
        content = decryptedContent,
        readBy = readByList,
        createdAt = createdAt,
    )
}


fun Message.toDomain(): DomainMessage {
    return DomainMessage(
        id = this._id.toHexString(),
        chatId = this.chatId.toHexString(),
        content = this.content,
        readBy = this.readBy.map { it.toHexString() },
        senderId = this.senderId.toHexString(),
        createdAt = this.createdAt!!.toJavaInstant()
    )
}


fun MessageDto.toDomain(decryptedContent: String): DomainMessage {
    return this.toRealm(decryptedContent).toDomain()
}