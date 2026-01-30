package com.example.projektiop.screens.friends

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.api.UserSearchDto
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.FriendItem
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.Result
import com.example.projektiop.ui.toStringRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- KONTRAKT (Stan i Zdarzenia) ---

data class FriendsUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendItem> = emptyList(),
    val incomingRequests: List<FriendItem> = emptyList(),
    val error: String? = null,

    val searchResults: List<UserSearchDto> = emptyList(),
    val isSearchLoading: Boolean = false,
    val searchError: String? = null,
    val sentRequests: Set<String> = emptySet(),
    val blockedUsers: List<FriendItem> = emptyList()
)

sealed interface FriendsUiEffect {
    data class NavigateToProfile(val route: String) : FriendsUiEffect
    data class NavigateToChat(val chatId: String, val friendId: String) : FriendsUiEffect
    data class ShowToast(val message: String) : FriendsUiEffect
    data class NavigateToReport(val friendId: String) : FriendsUiEffect
}

class FriendsViewModel(
    private val appContext: Context,
    private val friendshipRepository: FriendshipRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<FriendsUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()



    private val myId: String
        get() {
            return userRepository.myUser.value!!.id
        }

    init {
        refreshAll()
        viewModelScope.launch {
            combine(
                friendshipRepository.blockedIds,
                friendshipRepository.friendsIds,
                friendshipRepository.pendingIds
            ) { blocked, friends, pending ->
                Triple(blocked, friends, pending)
            }
                .distinctUntilChanged()
                .collect {
                    refreshAll()
                }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val friendsResult = friendshipRepository.fetchAccepted()
            val pendingResult = friendshipRepository.fetchIncomingPending()
            friendshipRepository.fetchBlocked()

            friendsResult.onSuccess { list ->
                _uiState.update { it.copy(friends = list) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }

            pendingResult.onSuccess { list ->
                _uiState.update { it.copy(incomingRequests = list) }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // --- Wyszukiwanie ---

    fun onSearchQueryChanged(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoading = true, searchError = null, searchResults = emptyList()) }

            userRepository.searchUsers(query)
                .onSuccess { results ->
                    _uiState.update { it.copy(searchResults = results) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(searchError = e.message) }
                }

            _uiState.update { it.copy(isSearchLoading = false) }
        }
    }

    fun onInviteUser(userId: String) {
        viewModelScope.launch {
            friendshipRepository.sendFriendRequest(userId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(sentRequests = state.sentRequests + userId)
                    }
                }
                .onFailure { e ->
                    _uiEffect.send(FriendsUiEffect.ShowToast(e.message ?: "Błąd wysyłania"))
                }
        }
    }

    fun clearSearchState() {
        _uiState.update {
            it.copy(
                searchResults = emptyList(),
                searchError = null,
                isSearchLoading = false
            )
        }
    }

    // --- Akcje z UI ---

    fun onFriendClicked(friend: FriendItem) {
        val encodedName = encode(friend.displayName)
        val encodedAvatar = encode(friend.avatarUrl)

        val route = "friend_profile/${friend.id}?username=${friend.username}&displayName=$encodedName&avatarUrl=$encodedAvatar"
        sendEffect(FriendsUiEffect.NavigateToProfile(route))
    }

    fun onChatClicked(friendId: String) {
        viewModelScope.launch {
            val ensureResult = chatRepository.ensureChatWithUser(friendId, myId)
            when(ensureResult) {
                is Result.Error -> {
                    val error = ensureResult.error
                    val errorMessage = appContext.getString(error.toStringRes())
                    sendEffect(FriendsUiEffect.ShowToast(errorMessage))
                }
                is Result.Success -> {
                    sendEffect(FriendsUiEffect.NavigateToChat(ensureResult.data.id, friendId))
                }
            }
        }
    }

    fun onRemoveFriend(friendshipId: String) {
        viewModelScope.launch {
            friendshipRepository.removeFriend(friendshipId)
                .onSuccess { refreshAll() }
                .onFailure { sendEffect(FriendsUiEffect.ShowToast("Błąd usuwania")) }
        }
    }

    fun onReportFriend(friendId: String) {
        sendEffect(FriendsUiEffect.NavigateToReport(friendId))
    }

    fun onBlockFriend(friendshipId: String) {
        viewModelScope.launch {
            friendshipRepository.blockFriendship(friendshipId)
                .onSuccess { refreshAll() }
                .onFailure { e ->  sendEffect(FriendsUiEffect.ShowToast("Błąd blokowania $e")) }
        }
    }

    fun loadBlocked() {
        viewModelScope.launch {
            friendshipRepository.fetchBlocked()
                .onSuccess { list ->
                    _uiState.update { it.copy(blockedUsers = list) }
                }
        }
    }

    fun onUnblockFriend (friendshipId: String) {
        viewModelScope.launch {
            friendshipRepository.unblockFriendship(friendshipId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            blockedUsers = state.blockedUsers.filterNot { it.friendshipId == friendshipId }
                        )
                    }
                }
        }
    }

    fun onAcceptRequest(friendshipId: String) {
        viewModelScope.launch {
            friendshipRepository.acceptFriendship(friendshipId).onSuccess { refreshAll() }.onFailure { refreshAll() }
        }
    }

    fun onRejectRequest(friendshipId: String) {
        viewModelScope.launch {
            friendshipRepository.rejectFriendship(friendshipId).onSuccess { refreshAll() }
        }
    }

    private fun sendEffect(effect: FriendsUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private fun encode(value: String?) =
        if (value != null) URLEncoder.encode(value, StandardCharsets.UTF_8.toString()) else ""
}