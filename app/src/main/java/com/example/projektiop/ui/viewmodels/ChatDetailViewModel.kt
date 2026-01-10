package com.example.projektiop.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.websocket.ChatEvent
import com.example.projektiop.data.api.websocket.SocketManager
import com.example.projektiop.data.repositories.BlockInfo
import com.example.projektiop.data.repositories.ChatListItem
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.models.Chat
import com.example.projektiop.domain.models.Friendship
import com.example.projektiop.domain.models.Message
import com.example.projektiop.util.DataError
import com.example.projektiop.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class ChatDetailViewModel(private val friendId: String,
                          private val chatRepository: ChatRepository,
                          private val userRepository: UserRepository,
                          private val friendshipRepository: FriendshipRepository,
                          private val socketManager: SocketManager): ViewModel() {

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

    val myId = userRepository.myUser.value!!._id.toHexString()
    private val _chat = MutableStateFlow<Chat?>(null)
    //private val _chatListItem = MutableStateFlow<ChatListItem?>(null)
    val chat: StateFlow<Chat?> = _chat.asStateFlow()

    val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneId.systemDefault())

    val chatEventFlow = socketManager.wsEventFlow

    init {
        ensureChat()
    }

    fun ensureChat() {
        viewModelScope.launch {
            val ensureResult = chatRepository.ensureChatWithUser(friendId, myId)
            when (ensureResult) {
                is Result.Error -> _errorMessage.value = when(ensureResult.error) {
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
                }
                is Result.Success -> {
                    _chat.value = ensureResult.data
                    getBlockInfo()
                    loadMessages()
                }
            }

            _loading.value = false
        }

        viewModelScope.launch {
            chatEventFlow.collect { event ->
                when(event) {
                    is ChatEvent.Receive -> {
                        chatRepository.decryptMessage(event.message).onSuccess { resultMess ->
                            _messages.value += resultMess
                            _typing.value = false
                        }
                    }
                    is ChatEvent.Send -> {}
                    is ChatEvent.Writing -> {
                        _typing.value = !typing.value }
                }
            }
        }
    }

    suspend fun getBlockInfo() {
        friendshipRepository.fetchBlocked()
        if (friendshipRepository.blockedIds.value.contains(friendId)) {
            val result = friendshipRepository.getLocalFriendship(friendId)
            var friendship: Friendship? = null
            when (result){
                is Result.Error -> _errorMessage.value = when(result.error) {
                    DataError.Local.DISK_FULL -> "no disk space"
                    DataError.Local.DB_ERROR -> "database error"
                    DataError.Local.NO_DATA -> "no local data"
                }
                is Result.Success -> friendship = result.data
            }
            val blockedByMe = friendship?.blockedBy == myId
            _blockInfo.value = BlockInfo(
                isBlocked = true,
                blockedByMe = blockedByMe,
                friendshipId = friendship?.id
            )
        }
    }

    suspend fun loadMessages() {
        chatRepository.loadMessages(chat.value!!.id).onSuccess { list ->
            _messages.value = list
        }.onFailure { e ->
            Log.d("mess", "failed to load messages:\t$e")
        }
    }

    fun onSendClick(content: String) {
        viewModelScope.launch {
            // chatRepository.sendMessage(chat.value!!.id, content)
            // loadMessages()
            Log.d("SOCC", "out send vm")
            chatRepository.sendMessageSocket(chat.value!!.id, content, socketManager)
        }
    }

    fun onType() {
        viewModelScope.launch {
            socketManager.emit(ChatEvent.Writing(chat.value!!.id))
        }
    }

    fun validateInput(input: String): Boolean {
        return input.isNotBlank()
    }
}