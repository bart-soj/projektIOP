package com.example.projektiop.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


sealed interface AppState {
    data object Unauthenticated : AppState
    data object Authenticated : AppState
    data object GotKeys : AppState
}


class AppStateRepository {
    private val _appState: MutableStateFlow<AppState> = MutableStateFlow(AppState.Unauthenticated)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    fun login() {
        if (_appState == AppState.Unauthenticated) {
            _appState.value = AppState.Authenticated
        }
    }

    fun logout() {
        _appState.value = AppState.Unauthenticated
    }

    fun gotKeys() {
        if (appState == AppState.Authenticated) {
            _appState.value = AppState.GotKeys
        }
    }
}