package com.example.projektiop

import android.app.Application
import com.example.projektiop.data.db.RealmProvider
import com.example.projektiop.data.repositories.AuthRepository
import io.realm.kotlin.Realm

/**
 * Application class for the HelloBeacon Application
 * It initializes the Realm Database and Repositories
 */

class HelloBeaconApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RealmProvider.init(this)
        AuthRepository.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        RealmProvider.close()
    }
}