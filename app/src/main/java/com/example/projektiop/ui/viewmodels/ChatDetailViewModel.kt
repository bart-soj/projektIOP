package com.example.projektiop.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.domain.ChatEvent
import com.example.projektiop.data.api.websocket.SocketManager
import com.example.projektiop.data.repositories.BlockInfo
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.models.Chat
import com.example.projektiop.domain.models.Friendship
import com.example.projektiop.domain.models.Message
import com.example.projektiop.domain.DataError
import com.example.projektiop.domain.Result
import com.example.projektiop.ui.toStringRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter


sealed interface ChatDetailUIEvent{
    data class ShowToast(val message: String) : ChatDetailUIEvent
    data class NavigateToReport(val friendId: String, val messageId: String, val content: String) : ChatDetailUIEvent
}


class ChatDetailViewModel(private val appContext: Context,
                          private val chatId: String,
                          private val friendId: String,
                          private val chatRepository: ChatRepository,
                          private val userRepository: UserRepository,
                          private val friendshipRepository: FriendshipRepository,
                          private val socketManager: SocketManager): ViewModel() {

    private val _uiEventChannel = Channel<ChatDetailUIEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _blockInfo = MutableStateFlow<BlockInfo?>(null)
    val blockInfo = _blockInfo.asStateFlow()

    private val _typing = MutableStateFlow(false)
    val typing: StateFlow<Boolean> = _typing.asStateFlow()

    val myUser = userRepository.myUser
    private val _chat = MutableStateFlow<Chat?>(null)
    //private val _chatListItem = MutableStateFlow<ChatListItem?>(null)
    val chat: StateFlow<Chat?> = _chat.asStateFlow()

    val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneId.systemDefault())

    val chatEventFlow = socketManager.chatEventFlow

    init {
        ensureChat()
        viewModelScope.launch {
            chatEventFlow.collect { event ->
                if(event.chatId == chatId || event.chatId == friendId) {
                    when(event) {
                        is ChatEvent.Receive -> {
                            chatRepository.decryptMessage(event.message, friendId).onSuccess { resultMess ->
                                _messages.value += resultMess
                                _typing.value = false
                            }.onFailure { e ->
                                Log.d("SOCC", "failed to decrypt ${event.message.content}", e)
                            }
                        }
                        is ChatEvent.Send -> {}
                        is ChatEvent.WritingStart -> {
                            Log.d("SOCC", "in writing start vm")
                            _typing.value = true }
                        is ChatEvent.WritingStop -> {
                            Log.d("SOCC", "in writing stop vm")
                            _typing.value = false }
                        is ChatEvent.Block -> {
                            Log.d("SOCC", "got block vm")
                            _blockInfo.value = BlockInfo(
                                isBlocked = true,
                                blockedByMe = false,
                            )
                        }
                        is ChatEvent.Unblock -> {
                            Log.d("SOCC", "got unblock vm")
                            _blockInfo.value = null
                        }
                    }
                }
            }
        }
    }

    fun ensureChat() {
        viewModelScope.launch {
            val ensureResult = chatRepository.ensureChatWithUser(friendId, myUser.value!!.id)
            when (ensureResult) {
                is Result.Error -> {
                    _errorMessage.value = appContext.getString(ensureResult.error.toStringRes())
                }
                is Result.Success -> {
                    _chat.value = ensureResult.data
                    getBlockInfo()
                    loadMessages()
                }
            }
            _loading.value = false
        }
    }

    suspend fun getBlockInfo() {
        friendshipRepository.fetchBlocked()
        if (friendshipRepository.blockedIds.value.contains(friendId)) {
            val result = friendshipRepository.getLocalFriendship(friendId)
            var friendship: Friendship? = null
            when (result){
                is Result.Error -> _errorMessage.value = appContext.getString(result.error.toStringRes())
                is Result.Success -> friendship = result.data
            }
            val blockedByMe = friendship?.blockedBy == myUser.value!!.id
            _blockInfo.value = BlockInfo(
                isBlocked = true,
                blockedByMe = blockedByMe
            )
        }
    }

    suspend fun loadMessages() {
        chatRepository.loadMessages(chatId, friendId).onSuccess { list ->
            _errorMessage.value = null
            _messages.value = list
        }.onFailure { e ->
            _errorMessage.value = "failed to load messages $e"
            Log.d("mess", "failed to load messages:\t$e")
        }
    }

    fun onSendClick(content: String) {
        viewModelScope.launch {

            chatRepository.sendMessageSocket(chatId, content)
        }
    }

    fun onTypeStart() {
        viewModelScope.launch {
            socketManager.emit(ChatEvent.WritingStart(chatId))
        }
    }

    fun onTypeStop() {
        viewModelScope.launch {
            socketManager.emit(ChatEvent.WritingStop(chatId))
        }
    }

    fun validateInput(input: String): Boolean {
        return input.isNotBlank()
    }

    fun onReportClick(messageId: String, content: String) {
        viewModelScope.launch {
            val encodedContent = Uri.encode(content)
            _uiEventChannel.send(ChatDetailUIEvent.NavigateToReport(friendId, messageId, encodedContent))
        }
    }
}