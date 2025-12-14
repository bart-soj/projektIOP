package com.example.projektiop.BluetoothLE

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val bleKoinModule = module {
    single { BluetoothRepository( androidContext()) }
    single { BTPermissionsManager( androidContext() ) }
}