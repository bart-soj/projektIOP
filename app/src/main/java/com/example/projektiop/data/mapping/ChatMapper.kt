package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.ChatDto
import com.example.projektiop.data.db.objects.Chat
import com.example.projektiop.util.mongoTimestampToRealmInstant
import io.realm.kotlin.types.RealmInstant

fun ChatDto.toRealm(): Chat {
    require(!this._id.isNullOrBlank()) { "Missing chat id" }
    val id = this._id
    val participants = this.participants
    require(!participants.isNullOrEmpty() ) { "Missing participants" }
    require(!this.lastMessage?._id.isNullOrBlank()) { "Missing last message id" }
    val lastMessageId = this.lastMessage._id
    val lastMessageTimestamp =
        mongoTimestampToRealmInstant(this.lastMessageTimestamp ?: (this.lastMessage.createdAt))
    val createdAt = mongoTimestampToRealmInstant(this.createdAt)

    return Chat.create(
        id = id,
        participants = participants,
        lastMessageId = lastMessageId,
        lastMessageTimestamp = lastMessageTimestamp,
        createdAt = createdAt,
        //updatedAt = RealmInstant.now()
    )
}