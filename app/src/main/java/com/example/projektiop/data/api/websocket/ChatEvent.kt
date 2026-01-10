package com.example.projektiop.data.api.websocket

import com.example.projektiop.domain.models.Message
import com.example.projektiop.domain.models.base64

sealed interface ChatEvent {
    val chatId: String
    class Send(override val chatId: String, val content: base64) : ChatEvent
    class Receive(override val chatId: String, val message: Message) : ChatEvent
    class Writing(override val chatId: String) : ChatEvent
}