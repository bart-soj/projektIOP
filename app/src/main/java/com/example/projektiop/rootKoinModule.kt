package com.example.projektiop

import android.app.Application
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.ChatUpdateService
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.data.repositories.ThemePreference
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.screens.friends.FriendsViewModel
import com.example.projektiop.ui.viewmodels.AuthViewModel
import com.example.projektiop.ui.viewmodels.ChatsViewModel
import com.example.projektiop.ui.viewmodels.FriendProfileViewModel
import com.example.projektiop.ui.viewmodels.MainViewModel
import com.example.projektiop.ui.viewmodels.ScannerViewModel
import com.example.projektiop.ui.viewmodels.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val rootKoinModule = module {
    single { UserRepository( get(), get(), get(), get() ) }
    single { InterestRepository( get(), get() ) }
    single { ThemePreference( get() ) }
    single { AuthRepository( get(), get() ) }
    single { ChatUpdateService }
    single { ChatRepository( get(), androidContext(), get(), get() ) }
    single { FriendshipRepository( get(), get(), get() ) }
    single { SharedDataSource( androidContext() ) }
    viewModel { MainViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get())}
    viewModel { AuthViewModel(get()) }
    viewModel { ScannerViewModel( get(), get(), get(), get(), get() ) }
    viewModel { FriendsViewModel( get(), get() ) }
    viewModel { ChatsViewModel(get(), get()) }
    viewModel { (userId: String) -> FriendProfileViewModel(userId, get()) }
}