package com.example.projektiop.domain.models

import com.google.gson.annotations.SerializedName
import java.time.Instant


class Chat(
    val id: String,
    val title: String,
    val participants: List<User>,
    val otherUserId: String,
    val lastMessage: Message?,
    val unread: Boolean,
    val lostHistory: Boolean,
    val createdAt: Instant,
) {}


class Message(
    @SerializedName("_id") val id: String,
    val chatId: String,
    val content: String,
    val readBy: List<String>,
    val senderId: String,
    val createdAt: Instant
) {}