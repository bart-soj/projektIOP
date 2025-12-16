package com.example.projektiop.data.db.realm

import android.content.Context
import com.example.projektiop.data.db.realm.objects.Friendship
import com.example.projektiop.data.db.realm.objects.Interest
import com.example.projektiop.data.db.realm.objects.InterestCategory
import com.example.projektiop.data.db.realm.objects.Message
import com.example.projektiop.data.db.realm.objects.SearchProfile
import com.example.projektiop.data.db.realm.objects.User
import com.example.projektiop.data.db.realm.objects.UserInterest
import com.example.projektiop.data.db.realm.objects.UserProfile
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

// TODO() use RealmSetTypes in RealmObject definition to represent relationships

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
                UserProfile::class,
                Interest::class,
                UserInterest::class,
                InterestCategory::class,
                SearchProfile::class
            )
        )
            .deleteRealmIfMigrationNeeded() // deletes db when it changes, only for development
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

