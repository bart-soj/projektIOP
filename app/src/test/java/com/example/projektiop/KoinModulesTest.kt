package com.example.projektiop

import android.app.Application
import android.content.Context
import com.example.projektiop.BluetoothLE.bleKoinModule
import com.example.projektiop.activeHandshake.NFC.nfcKoinModule
import com.example.projektiop.data.api.apiKoinModule
import com.example.projektiop.data.db.realm.realmKoinModule
import com.example.projektiop.ui.viewmodels.viewModelsKoinModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

class KoinModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun verifyKoinModules() {
        val allModule = module {
            includes (
                rootKoinModule,
                bleKoinModule,
                apiKoinModule,
                realmKoinModule,
                nfcKoinModule,
                viewModelsKoinModule
            )
        }


        allModule.verify(extraTypes = listOf(String::class, Application::class, Context::class))
    }
}
