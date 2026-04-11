package com.example.projektiop

import com.example.projektiop.data.SharedDataSource
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.ChatUpdateService
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.LanguageRepository
import com.example.projektiop.data.repositories.OtherUserRepository
import com.example.projektiop.data.repositories.SearchProfileRepository
import com.example.projektiop.data.repositories.ThemePreference
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.AppStateRepository
import com.example.projektiop.data.util.KeyUtils
import com.example.projektiop.domain.InterestSimilarity
import com.example.projektiop.domain.OtsukaOchiaiSimilarityImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val rootKoinModule = module {
    single { UserRepository( get(), get(), get(), get() ) }
    single { InterestRepository( get(), get(), get() ) }
    single { ThemePreference( get() ) }
    single { AuthRepository( get(), get(), get(), get() ) }
    single { ChatUpdateService }
    single { ChatRepository( get(), get(), get(), get(), get() ) }
    single { FriendshipRepository( get(), get(), get() ) }
    single { SharedDataSource( androidContext() ) }
    single { SearchProfileRepository( get(), get() ) }
    single { KeyUtils( get(), get(), get(), get() ) }
    single { AppStateRepository() }
    single { LanguageRepository( get() ) }
    factory { (id: String) ->
        OtherUserRepository.ensureRepository(id, get(), get(), get())
    }
    single<InterestSimilarity> { OtsukaOchiaiSimilarityImpl() }
}