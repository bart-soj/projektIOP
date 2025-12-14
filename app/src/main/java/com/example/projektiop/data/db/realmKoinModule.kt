package com.example.projektiop.data.db

import com.example.projektiop.data.repositories.RealmDBRepository
import io.realm.kotlin.Realm
import org.koin.dsl.module

val realmKoinModule = module {
    single { RealmDBRepository( get() ) }
    single<Realm> { RealmProvider.getRealm() }
}
