package com.example.projektiop.domain.models

import java.time.Instant


class Chat(
    val id: String,
    val participants: List<User>,
    val lastMessage: Message,
    val createdAt: Instant
) {}


class Message(
    val id: String,
    val chatId: String,
    val content: String,
    val readBy: List<String>,
    val sender: String,
    val createdAt: Instant
) {}