package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.ChatDto
import com.example.projektiop.data.db.objects.Chat
import io.realm.kotlin.ext.realmListOf

fun ChatDto.toRealm(): Chat {
    require(!this._id.isNullOrBlank()) { "Missing chat id" }
    val id = this._id
    val participants = realmListOf<String>().apply {
        addAll(this@toRealm.participants?.mapNotNull { it._id } ?: emptyList())
    }
    require(participants.isNotEmpty()) { "Missing participants" }
    require(!this.lastMessage?._id.isNullOrBlank()) { "Missing last message id" }
    val lastMessageId = this.lastMessage._id
    val lastMessageTimestamp = mongoTimestampToRealmInstant(this.lastMessage.createdAt)

    return Chat.create(
        id = id,
        participants = participants,
        lastMessageId = lastMessageId,
        lastMessageTimestamp = lastMessageTimestamp,
        // createdAt = TODO() not present in dto
        // updatedAt = TODO() not present in dto
    )
}