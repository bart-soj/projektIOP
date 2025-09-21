package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.MessageDto
import com.example.projektiop.data.db.objects.Message
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import com.google.gson.JsonElement

fun MessageDto.toRealm(): Message {
    var message = Message()

    message.id = this._id.toString()
    message.chatId = extractIdFromElement(this.chatId)
    message.senderId = this.senderId?._id ?: ""
    message.content = this.content ?: ""
    message.readBy = (this.readBy?.mapNotNull { it._id } ?: emptyList<String>()) as RealmList<String>
    message.createdAt = mongoTimestampToRealmInstant(this.createdAt)

    return message
}

private fun extractIdFromElement(el: JsonElement?): String {
    if (el == null || el.isJsonNull) return ""
    return try {
        when {
            el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString
            el.isJsonObject && el.asJsonObject.has("_id") -> el.asJsonObject.get("_id").asString
            else -> el.toString()
        }
    } catch (_: Exception) {
        ""
    }
}