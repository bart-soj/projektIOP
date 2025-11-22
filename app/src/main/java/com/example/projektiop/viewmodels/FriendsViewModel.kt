package com.example.projektiop.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.data.repositories.FriendItem
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.PendingRequestItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- KONTRAKT (Stan i Zdarzenia) ---

data class FriendsUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendItem> = emptyList(),
    val incomingRequests: List<PendingRequestItem> = emptyList(),
    val error: String? = null
)

sealed interface FriendsUiEffect {
    data class NavigateToProfile(val route: String) : FriendsUiEffect
    data class NavigateToChat(val friendId: String) : FriendsUiEffect
    data class ShowToast(val message: String) : FriendsUiEffect
}

// --- VIEWMODEL ---

class FriendsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<FriendsUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Odwołujemy się bezpośrednio do Twoich obiektów Repositories
            val friendsResult = FriendshipRepository.fetchAccepted()
            val pendingResult = FriendshipRepository.fetchIncomingPending()

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

    // --- Akcje z UI ---

    fun onFriendClicked(friend: FriendItem) {
        // Logika budowania URL przeniesiona tutaj
        val encodedName = encode(friend.displayName)
        val encodedAvatar = encode(friend.avatarUrl)

        val route = "friend_profile/${friend.id}?username=${friend.username}&displayName=$encodedName&avatarUrl=$encodedAvatar"
        sendEffect(FriendsUiEffect.NavigateToProfile(route))
    }

    fun onChatClicked(friendId: String) {
        sendEffect(FriendsUiEffect.NavigateToChat(friendId))
    }

    fun onRemoveFriend(friendshipId: String) {
        viewModelScope.launch {
            FriendshipRepository.removeFriend(friendshipId)
                .onSuccess { refreshAll() }
                .onFailure { sendEffect(FriendsUiEffect.ShowToast("Błąd usuwania")) }
        }
    }

    fun onBlockFriend(friendshipId: String) {
        viewModelScope.launch {
            FriendshipRepository.blockFriendship(friendshipId).onSuccess { refreshAll() }
        }
    }

    fun onAcceptRequest(friendshipId: String) {
        viewModelScope.launch {
            FriendshipRepository.acceptFriendship(friendshipId).onSuccess { refreshAll() }
        }
    }

    fun onRejectRequest(friendshipId: String) {
        viewModelScope.launch {
            FriendshipRepository.rejectFriendship(friendshipId).onSuccess { refreshAll() }
        }
    }

    private fun sendEffect(effect: FriendsUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private fun encode(value: String?) =
        if (value != null) URLEncoder.encode(value, StandardCharsets.UTF_8.toString()) else ""
}