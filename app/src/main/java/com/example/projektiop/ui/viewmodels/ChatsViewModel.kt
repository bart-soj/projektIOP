package com.example.projektiop.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.ChatListItem
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.DataError
import com.example.projektiop.domain.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.collections.filter
import com.example.projektiop.domain.models.Chat as DomainChat

@OptIn(ExperimentalCoroutinesApi::class)
class ChatsViewModel(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _chats = MutableStateFlow<List<DomainChat>>(emptyList())

    val filteredChats: StateFlow<List<ChatListItem>> =
        combine(_chats, searchText) { chats, searchText ->
        if (searchText.isNotBlank()) {
            chats.filter {
                (it.title.contains(searchText, ignoreCase = true) ||
                        (it.lastMessage?.content?:"").contains(searchText, ignoreCase = true))
            }.sortedByDescending { it.lastMessage?.createdAt ?: Instant.MIN }.map {
                ChatListItem(
                    id = it.id,
                    title = it.title,
                    lastMessage = it.lastMessage?.content ?: "" ,
                    lastMessageTime = it.lastMessage?.createdAt.toString(),
                    friendId = it.otherUserId,
                    avatarUrl = it.participants.first{ user ->  user.id == it.otherUserId }.profile.avatarUrl,
                    unread = it.unread
                )
            }
        } else {
            chats.sortedByDescending { it.lastMessage?.createdAt ?: Instant.MIN }.map {
                ChatListItem(
                    id = it.id,
                    title = it.title,
                    lastMessage = it.lastMessage?.content ?: "" ,
                    lastMessageTime = it.lastMessage?.createdAt.toString(),
                    friendId = it.otherUserId,
                    avatarUrl = it.participants.first{ user ->  user.id == it.otherUserId }.profile.avatarUrl,
                    unread = it.unread
                )
            }
        }
    }.stateIn (
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _chats.value.sortedByDescending { it.lastMessage?.createdAt ?: Instant.MIN }.map {
            ChatListItem(
                id = it.id,
                title = it.title,
                lastMessage = it.lastMessage?.content ?: "" ,
                lastMessageTime = it.lastMessage?.createdAt.toString(),
                friendId = it.otherUserId,
                avatarUrl = it.participants.first{ user ->  user.id == it.otherUserId }.profile.avatarUrl,
                unread = it.unread
            )
        }
    )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val myUser = userRepository.myUser

    init {
        viewModelScope.launch {
            chatRepository.chats.flatMapLatest{it}.collect { newList ->
                _chats.value = newList
            }
        }
        refreshAll()
    }

    fun refreshAll() {
        _loading.value = true
        viewModelScope.launch {
            val result = chatRepository.fetchChats( myUser.value!!.id )
            when(result) {
                is Result.Error -> _error.value = when (result.error) {
                    DataError.Local.DISK_FULL -> "no disk space"
                    DataError.Local.DB_ERROR -> "db failed"
                    DataError.Network.REQUEST_TIMEOUT -> "request timeout"
                    DataError.Network.TOO_MANY_REQUESTS -> "Server Error"
                    DataError.Network.NO_INTERNET -> "no internet"
                    DataError.Network.PAYLOAD_TOO_LARGE -> "Server Error"
                    DataError.Network.SERVER_ERROR -> "Server Error"
                    DataError.Network.SERIALIZATION -> "Serialization Error"
                    DataError.Network.UNKNOWN -> "Unknown Error"
                    DataError.Local.NO_DATA -> "no local data"
                    DataError.Authentication.INVALID_EMAIL_PASSWORD -> "Invalid Email or Password"
                    DataError.Authentication.ACCOUNT_BANNED -> "Account banned"
                    DataError.Authentication.EMAIL_NOT_VERIFIED -> "Email not verified"
                    DataError.Authentication.EMAIL_USERNAME_TAKEN -> "Username or Email taken"
                }
                is Result.Success -> {
                    _chats.value = result.data
                }
            }
            _loading.value = false
        }
    }

    fun searchFor(string: String) {
        _searchText.value = string
    }
}