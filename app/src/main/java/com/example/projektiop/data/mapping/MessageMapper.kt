package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.MessageDto
import com.example.projektiop.data.db.objects.Message
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import com.google.gson.JsonElement
import io.realm.kotlin.ext.realmListOf

fun MessageDto.toRealm(): Message {
    val id = this._id ?: throw IllegalArgumentException("Missing message id")
    val chatId = extractIdFromElement(this.chatId).takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Missing chat id")
    val senderId = this.senderId?._id ?: throw IllegalArgumentException("Missing sender id")

    val readByList = realmListOf<String>().apply {
        addAll(this@toRealm.readBy?.mapNotNull { it._id } ?: emptyList())
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
        updatedAt = updatedAt
    )
}

private fun extractIdFromElement(el: JsonElement?): String {
    if (el == null || el.isJsonNull) return ""
    return try {
        when {
            el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString
            el.isJsonObject && el.asJsonObject.has("_id") -> el.asJsonObject.get("*id").asString
            else -> el.toString()
        }
    } catch (_: Exception) {
        ""
    }
}