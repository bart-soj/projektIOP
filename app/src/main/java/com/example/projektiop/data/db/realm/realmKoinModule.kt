package com.example.projektiop.data.db.realm

import io.realm.kotlin.Realm
import org.koin.dsl.module

val realmKoinModule = module {
    single { RealmDataSource( get() ) }
    single<Realm> { RealmProvider.getRealm() }
}
