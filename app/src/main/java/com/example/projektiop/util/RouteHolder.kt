package com.example.projektiop.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RouteHolder {
    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute = _currentRoute.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId = _currentChatId.asStateFlow()

    fun setRoute(route: String?, chatId: String?) {
        _currentRoute.value = route
        _currentChatId.value = chatId
    }
}