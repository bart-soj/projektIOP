package com.example.projektiop.ui.viewmodels

import com.example.projektiop.screens.friends.FriendsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelsKoinModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get())}
    viewModel { AuthViewModel(get()) }
    viewModel { ScannerViewModel( get(), get(), get(), get(), get(), get() ) }
    viewModel { FriendsViewModel( get(), get() ) }
    viewModel { ChatsViewModel(get(), get()) }
    viewModel { (userId: String) -> FriendProfileViewModel(userId, get()) }
    viewModel { (friendId: String) -> ChatDetailViewModel(friendId, get(), get(), get(), get()) }
    viewModel { KeyLoadingViewModel( get(), get(), get() ) }
    viewModel { EditProfileViewModel( get(), get() ) }
    viewModel { BackupDialogViewModel( get(), get(), get() ) }
    viewModel { ReportViewModel( get(), get() ) }
    viewModel { ResendEmailViewModel( get()) }
}