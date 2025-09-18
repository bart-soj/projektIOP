package com.example.projektiop

import android.app.Application
import com.example.projektiop.data.db.RealmProvider
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.ThemePreference
import com.example.projektiop.data.repositories.DBRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.util.NotificationHelper
import io.realm.kotlin.Realm

/**
 * Application class for the HelloBeacon Application
 * It initializes the Realm Database and Repositories
 */

class HelloBeaconApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RealmProvider.init(this)
        DBRepository.init(RealmProvider.getRealm())
        SharedPreferencesRepository.init(this)
        AuthRepository.init(this)
        UserRepository.init(this)
        NotificationHelper.initChannels(this)
        com.example.projektiop.data.repositories.ChatRepository.init(this)
        com.example.projektiop.data.repositories.ChatUpdateManager.start(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        RealmProvider.close()
    }
}