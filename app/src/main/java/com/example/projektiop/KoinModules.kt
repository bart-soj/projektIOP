package com.example.projektiop

import android.app.Application
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.ChatUpdateManager
import com.example.projektiop.data.repositories.DBRepository
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.data.repositories.ThemePreference
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.screens.friends.FriendsViewModel
import com.example.projektiop.ui.viewmodels.AuthViewModel
import com.example.projektiop.ui.viewmodels.ChatsViewModel
import com.example.projektiop.ui.viewmodels.MainViewModel
import com.example.projektiop.ui.viewmodels.ScannerViewModel
import com.example.projektiop.ui.viewmodels.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.Scanner

val rootKoinModule = module {
    single { UserRepository }
    single { ThemePreference }
    single { DBRepository }
    single { AuthRepository }
    single { AuthRepository }
    single { ChatUpdateManager }
    single { ChatRepository }
    single { SharedPreferencesRepository }
    single { FriendshipRepository }
    viewModel { MainViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get())}
    viewModel { AuthViewModel(get()) }
    viewModel { ScannerViewModel(get<Application>(), get(), get()) }
    viewModel { FriendsViewModel(get()) }
    viewModel { ChatsViewModel(get(), get(), get()) }
}