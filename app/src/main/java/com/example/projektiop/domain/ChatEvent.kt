package com.example.projektiop.domain

import com.example.projektiop.domain.models.Message
import com.example.projektiop.domain.models.base64

sealed interface ChatEvent {
    val chatId: String
    class Send(override val chatId: String, val content: base64) : ChatEvent
    class Receive(override val chatId: String, val message: Message) : ChatEvent
    class WritingStart(override val chatId: String) : ChatEvent
    class WritingStop(override val chatId: String) : ChatEvent
    class Block(override val chatId: String) : ChatEvent
    class Unblock(override val chatId: String) : ChatEvent
}