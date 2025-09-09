package com.example.projektiop.data.db

import android.content.Context
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.db.objects.Friendship
import com.example.projektiop.data.db.objects.UserInterest
import com.example.projektiop.data.db.objects.Message
import com.example.projektiop.data.db.objects.UserProfile

object RealmProvider {
    private lateinit var realmInstance: Realm

    fun init(context: Context) {
        if (::realmInstance.isInitialized) return // Prevent re-initialization

        val config = RealmConfiguration.Builder(
            schema = setOf(
                Message::class,
                UserInterest::class,
                Friendship::class,
                User::class,
                UserProfile::class
            )
        )
            .schemaVersion(1)
            .build()

        realmInstance = Realm.open(config)
    }

    fun getRealm(): Realm {
        check(::realmInstance.isInitialized) { "RealmProvider not initialized. Call RealmProvider.init() first." }
        return realmInstance
    }

    fun close() {
        if (::realmInstance.isInitialized) {
            realmInstance.close()
        }
    }
}