package com.example.projektiop.data.util

import io.realm.kotlin.types.RealmInstant
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale


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


fun RealmInstant.toJavaInstant(): Instant {
    return Instant.ofEpochSecond(
        this.epochSeconds,
        this.nanosecondsOfSecond.toLong()
    )
}


fun isoDateStringToMillis(date: String): Long {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.parse(date)?.time ?: 0L
}


fun isoToDisplayDate(isoDate: String): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val date = inputFormat.parse(isoDate)
    return outputFormat.format(date!!)
}

