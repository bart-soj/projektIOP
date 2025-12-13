package com.example.projektiop.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.repositories.ChatListItem
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.ChatUpdateManager
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.filter

class ChatsViewModel(private val userRepository: UserRepository,
                     private val chatUpdateManager: ChatUpdateManager,
                     private val chatRepository: ChatRepository) : ViewModel() {
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    val _chats = MutableStateFlow<List<ChatListItem>>(emptyList())
    val filteredChats: StateFlow<List<ChatListItem>> = combine(_chats.asStateFlow(), searchText) { chats, searchText ->
        if (searchText.isNotBlank()) {
            chats.filter {
                (it.title.contains(searchText, ignoreCase = true) || it.lastMessage.contains(searchText, ignoreCase = true))
            }
        } else {
            chats
        }
    }.stateIn (scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())



    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val _myUser = MutableStateFlow<User?>(null)
    val myUser = _myUser.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.myUserFlow.collect { value ->
                _myUser.value = value
            }
            chatUpdateManager.chatsFlow.collect { value ->
                _chats.value = value
            }
        }
    }

    fun refreshAll() {
        _loading.value = true
        viewModelScope.launch {
            chatRepository.fetchChats(currentUserId = myUser.value?._id?.toHexString(),
                currentUsername = myUser.value?.username)
                .onSuccess { _chats.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun searchFor(string: String) {
        _searchText.value = string
    }
}