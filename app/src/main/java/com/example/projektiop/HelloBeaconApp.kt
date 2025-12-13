package com.example.projektiop

import android.app.Application
import com.example.projektiop.BluetoothLE.BluetoothRepository
import com.example.projektiop.activeHandshake.NFC.nfcKoinModule
import com.example.projektiop.data.db.RealmProvider
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.DBRepository
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

/**
 * Application class for the HelloBeacon Application
 * It initializes the Realm Database and Repositories
 */

class HelloBeaconApp : Application() {

    val bluetoothRepository by lazy { BluetoothRepository(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@HelloBeaconApp)
            modules(rootKoinModule, nfcKoinModule)
        }

        RealmProvider.init(this)
        DBRepository.init(RealmProvider.getRealm())
        SharedPreferencesRepository.init(this)
        UserRepository.init()
        applicationScope.launch{
            AuthRepository.init()
            InterestRepository.init()
        }
        NotificationHelper.initChannels(this)
        com.example.projektiop.data.repositories.ChatRepository.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        RealmProvider.close()
        AuthRepository.onTerminate()
    }
}