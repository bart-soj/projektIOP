package com.example.projektiop.ui

import com.example.projektiop.screens.friends.FriendsViewModel
import com.example.projektiop.ui.viewmodels.AuthViewModel
import com.example.projektiop.ui.viewmodels.BackupViewModel
import com.example.projektiop.ui.viewmodels.ChatDetailViewModel
import com.example.projektiop.ui.viewmodels.ChatsViewModel
import com.example.projektiop.ui.viewmodels.EditProfileViewModel
import com.example.projektiop.ui.viewmodels.FriendProfileViewModel
import com.example.projektiop.ui.viewmodels.KeyLoadingViewModel
import com.example.projektiop.ui.viewmodels.LanguageViewModel
import com.example.projektiop.ui.viewmodels.MainViewModel
import com.example.projektiop.ui.viewmodels.ReportViewModel
import com.example.projektiop.ui.viewmodels.ResendEmailViewModel
import com.example.projektiop.ui.viewmodels.ScannerViewModel
import com.example.projektiop.ui.viewmodels.SettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelsKoinModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { SettingsViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { AuthViewModel(androidApplication(), get()) }
    viewModel { ScannerViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { FriendsViewModel(androidApplication(), get(), get(), get() ) }
    viewModel { ChatsViewModel(get(), get()) }
    viewModel { (userId: String, uname: String, dname: String, avatar: String) ->
        FriendProfileViewModel(userId, uname, dname, avatar, get()) }
    viewModel { (chatId: String, friendId: String) -> ChatDetailViewModel(androidApplication(), chatId, friendId, get(), get(), get(), get()) }
    viewModel { KeyLoadingViewModel(androidApplication(), get(), get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
    viewModel { BackupViewModel(androidApplication(), get(), get(), get()) }
    viewModel { ReportViewModel(androidApplication(), get(), get()) }
    viewModel { ResendEmailViewModel(androidApplication(), get()) }
    viewModel { LanguageViewModel(get(), get()) }
}