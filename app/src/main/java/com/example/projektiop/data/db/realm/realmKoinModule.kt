package com.example.projektiop.data.db.realm

import io.realm.kotlin.Realm
import org.koin.dsl.module

val realmKoinModule = module {
    single<RealmProvider> { RealmProvider( get() ) }
    single<Realm> { get<RealmProvider>().getRealm() }
    single { RealmDataSource( get() ) }
}
