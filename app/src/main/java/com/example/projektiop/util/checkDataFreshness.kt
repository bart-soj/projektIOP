package com.example.projektiop.util

import io.realm.kotlin.types.RealmInstant
import java.time.Duration
import java.time.Instant


fun checkDataFreshness(updatedAt: RealmInstant, allowedAge: Duration): Boolean {
    val instant = updatedAt.toJavaInstant()
    val expiry = instant.plus(allowedAge)
    return expiry.isBefore(Instant.now())
}