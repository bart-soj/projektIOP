package com.example.projektiop.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow


sealed interface AppState {
    data object Unauthenticated : AppState
    data object Authenticated : AppState
    data object GotKeys : AppState
}


sealed interface AppStateEvent {
    data object OnAuthorization: AppStateEvent
    data object OnLogout: AppStateEvent
    data object OnGotKeys: AppStateEvent
}


class AppStateRepository {
    private val _appState: MutableStateFlow<AppState> = MutableStateFlow(AppState.Unauthenticated)
    val appState: StateFlow<AppState> = _appState.asStateFlow()


    private val _appStateEventFlow = MutableSharedFlow<AppStateEvent>()
    val appStateEventFlow = _appStateEventFlow.asSharedFlow()

    suspend fun login() {
        if (appState.value == AppState.Unauthenticated) {
            _appStateEventFlow.emit(AppStateEvent.OnAuthorization)
            _appState.value = AppState.Authenticated
        }
    }

    suspend fun logout() {
        _appStateEventFlow.emit(AppStateEvent.OnLogout)
        _appState.value = AppState.Unauthenticated
    }

    suspend fun gotKeys() {
        if (appState.value == AppState.Authenticated) {
            _appStateEventFlow.emit(AppStateEvent.OnGotKeys)
            _appState.value = AppState.GotKeys
        }
    }
}