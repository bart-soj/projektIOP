package com.example.projektiop.data.api.websocket

sealed interface AppEvent {
    object Ban : AppEvent
    data class Block(val friendId: String) : AppEvent
    data class Unblock(val friendId: String) : AppEvent
}