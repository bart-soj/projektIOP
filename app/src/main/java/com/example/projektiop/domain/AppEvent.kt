package com.example.projektiop.domain

import com.example.projektiop.domain.models.Message

sealed interface AppEvent {
    object Ban : AppEvent
    data class Block(val friendId: String) : AppEvent
    data class Unblock(val friendId: String) : AppEvent
    data class Receive(val message: Message) : AppEvent
    data class Invite(val userId: String) : AppEvent
    data class InviteRejected(val friendshipId: String) : AppEvent
    data class InviteAccepted(val friendshipId: String) : AppEvent
    data class FriendshipEnded(val friendshipId: String) : AppEvent
}