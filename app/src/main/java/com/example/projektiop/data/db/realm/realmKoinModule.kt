package com.example.projektiop.data.db.realm

import com.example.projektiop.data.db.realm.RealmDBRepository
import io.realm.kotlin.Realm
import org.koin.dsl.module

val realmKoinModule = module {
    single { RealmDBRepository( get() ) }
    single<Realm> { RealmProvider.getRealm() }
}
