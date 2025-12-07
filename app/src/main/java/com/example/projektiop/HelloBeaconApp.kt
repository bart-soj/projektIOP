package com.example.projektiop

import android.app.Application
import com.example.projektiop.BluetoothLE.BluetoothRepository
import com.example.projektiop.data.db.RealmProvider
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.ChatUpdateManager
import com.example.projektiop.data.repositories.DBRepository
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Application class for the HelloBeacon Application
 * It initializes the Realm Database and Repositories
 */

class HelloBeaconApp : Application() {

    val bluetoothRepository by lazy { BluetoothRepository(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        RealmProvider.init(this)
        DBRepository.init(RealmProvider.getRealm())
        SharedPreferencesRepository.init(this)
        UserRepository.init(this)
        AuthRepository.init(this)
        applicationScope.launch{
            InterestRepository.init()
        }
        // ChatUpdateManager.start(this)
        NotificationHelper.initChannels(this)
        com.example.projektiop.data.repositories.ChatRepository.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        RealmProvider.close()
    }
}