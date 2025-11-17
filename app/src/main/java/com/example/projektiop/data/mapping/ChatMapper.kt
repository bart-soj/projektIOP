package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.ChatDto
import com.example.projektiop.data.db.objects.Chat
import io.realm.kotlin.ext.realmListOf

fun ChatDto.toRealm(): Chat {
    val id = this._id ?: throw IllegalArgumentException("Missing chat id")
    val participants = realmListOf<String>().apply {
            addAll(this@toRealm.participants?.mapNotNull { it._id } ?: emptyList())
        }
    val lastMessageId = this.lastMessage?._id ?: throw IllegalArgumentException("Missing last message id")
    val lastMessageTimestamp = mongoTimestampToRealmInstant(this.lastMessage.createdAt)

    return Chat.create(
        id = id,
        participants = participants,
        lastMessageId = lastMessageId,
        lastMessageTimestamp = lastMessageTimestamp,
        createdAt = TODO(), // not present in dto
        updatedAt = TODO() // not present in dto
    )
}