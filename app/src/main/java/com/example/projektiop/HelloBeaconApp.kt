package com.example.projektiop

import android.app.Application
import com.example.projektiop.BluetoothLE.bleKoinModule
import com.example.projektiop.activeHandshake.NFC.nfcKoinModule
import com.example.projektiop.data.api.apiKoinModule
import com.example.projektiop.data.db.realm.RealmProvider
import com.example.projektiop.data.db.realm.realmKoinModule
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.LanguageRepository
import com.example.projektiop.ui.viewModelsKoinModule
import com.example.projektiop.util.NotificationHelper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

/**
 * Application class for the HelloBeacon Application
 * It initializes the RealmProvider and NotificationHelper
 */

class HelloBeaconApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@HelloBeaconApp)
            modules(rootKoinModule, nfcKoinModule, apiKoinModule, realmKoinModule, bleKoinModule,
                viewModelsKoinModule
            )
        }

        NotificationHelper.initChannels(this)

        // initializes language before MainActivity is created
        getKoin().get<LanguageRepository>()

        val realmProvider by inject<RealmProvider>()
        realmProvider.init(this)
    }


    override fun onTerminate() {
        super.onTerminate()
        val authRepository by inject<AuthRepository>()
        val realmProvider by inject<RealmProvider>()
        realmProvider.close()
        MainScope().launch {
            authRepository.onTerminate()
        }
    }
}