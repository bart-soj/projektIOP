package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.ChatDto
import com.example.projektiop.data.db.realm.RealmDataSource
import com.example.projektiop.data.db.realm.objects.Chat
import com.example.projektiop.data.db.realm.objects.User
import com.example.projektiop.domain.models.base64
import com.example.projektiop.data.util.mongoTimestampToRealmInstant
import com.example.projektiop.data.util.toJavaInstant
import java.time.Instant
import com.example.projektiop.domain.models.Chat as DomainChat
import com.example.projektiop.domain.models.Message as DomainMessage

fun ChatDto.toRealm(chatKey: base64): Chat {
    require(!this._id.isNullOrBlank()) { "Missing chat id" }
    val id = this._id
    val participants = this.participants
    require(!participants.isNullOrEmpty() ) { "Missing participants" }
    val lastMessageId = this.lastMessage?._id
    val lastMessageTimestamp =
        mongoTimestampToRealmInstant(this.lastMessageTimestamp ?: (this.lastMessage?.createdAt))
    val createdAt = mongoTimestampToRealmInstant(this.createdAt)
    val lostHistory = lastResetDate != null

    return Chat.create(
        id = id,
        participants = participants,
        lastMessageId = lastMessageId,
        chatKey = chatKey,
        lastMessageTimestamp = lastMessageTimestamp,
        lostHistory = lostHistory,
        createdAt = createdAt,
    )
}


fun Chat.toDomain(myUserId: String, dbRepository: RealmDataSource, message: DomainMessage? = null): DomainChat {
    /*
    var lastMessage: Message? = null
    if (this.lastMessageId != null) {
        lastMessage = dbRepository.getMessageById(this.lastMessageId!!)
    }
     */

    val participants: List<User> = this.participants.map {
        dbRepository.getUserById(it)!!
    }

    // val unread = lastMessage?.readBy?.map{it.toHexString()}?.contains(myUserId)
    val unread = message?.readBy?.contains(myUserId)
    val otherUser = participants.firstOrNull{ user -> user._id.toHexString() != myUserId}
    val otherUserName = otherUser?.profile?.displayName ?: otherUser?.username
    val lostHistory = this.lostHistory

    return DomainChat(
        id = this._id.toHexString(),
        title = otherUserName.toString(),
        participants = participants.map{ it.toDomain() },
        // lastMessage = lastMessage?.toDomain(),
        lastMessage = message,
        unread = unread == true,
        createdAt = this.createdAt?.toJavaInstant() ?: Instant.now(), // TODO() hotifx
        otherUserId = otherUser!!._id.toHexString(),
        lostHistory = lostHistory
    )
}