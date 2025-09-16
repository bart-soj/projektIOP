package com.example.projektiop.data.mapping

import io.realm.kotlin.types.RealmInstant
import java.time.Instant
import java.time.OffsetDateTime


// Convert MongoDB timestamp string (ISO-8601 with Z or offset) -> RealmInstant
fun mongoTimestampToRealmInstant(timestamp: String?): RealmInstant? {
    if (timestamp.isNullOrBlank()) return null
    return try {
        val instant = OffsetDateTime.parse(timestamp).toInstant()
        RealmInstant.from(instant.epochSecond, instant.nano)
    } catch (e: Exception) {
        null
    }
}


// Convert RealmInstant -> MongoDB timestamp string (ISO-8601 in UTC with Z)
fun realmInstantToMongoTimestamp(realmInstant: RealmInstant?): String? {
    if (realmInstant == null) return null
    val instant = Instant.ofEpochSecond(
        realmInstant.epochSeconds,
        realmInstant.nanosecondsOfSecond.toLong()
    )
    return instant.toString()
}