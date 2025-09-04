package com.example.projektiop.data

import com.example.projektiop.api.RetrofitInstance
import com.example.projektiop.api.UserSearchDto
import com.example.projektiop.api.FriendRequest
import com.example.projektiop.api.FriendshipDto
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FriendItem(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?
)

data class PendingRequestItem(
    val friendshipId: String,
    val userId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?
)

object FriendshipRepository {
    suspend fun fetchAccepted(): Result<List<FriendItem>> = withContext(Dispatchers.IO) {
        // Nie używamy status=accepted, bo backend wtedy automatycznie filtruje tylko 'verified'.
        // Pobieramy bez status i filtrujemy lokalnie, aby pokazać też 'unverified'.
        try {
            val response = RetrofitInstance.friendshipApi.getFriendships()
            if (response.isSuccessful) {
                val all = response.body().orEmpty().filter { it.status == "accepted" }
                Result.success(all.mapNotNull { mapFriendDto(it) })
            } else Result.failure(Exception("Nie udało się pobrać listy (${response.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchIncomingPending(): Result<List<PendingRequestItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitInstance.friendshipApi.getFriendships(status = "pending", direction = "incoming")
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("Błąd pobierania zaproszeń (${resp.code()})"))
            val items = resp.body().orEmpty()
                .filter { it.isPendingRecipient == true || it.status == "pending" }
                .mapNotNull { dto ->
                    val ref = dto.user ?: dto.userId ?: dto.friendId ?: return@mapNotNull null
                    PendingRequestItem(
                        friendshipId = dto.friendshipId ?: dto._id ?: return@mapNotNull null,
                        userId = ref._id ?: return@mapNotNull null,
                        displayName = ref.profile?.displayName ?: ref.username ?: "(bez nazwy)",
                        username = ref.username ?: "",
                        avatarUrl = ref.profile?.avatarUrl
                    )
                }
            Result.success(items)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun acceptFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitInstance.friendshipApi.acceptRequest(friendshipId)
            if (r.isSuccessful) Result.success(Unit) else Result.failure(Exception("Błąd akceptacji (${r.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun rejectFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitInstance.friendshipApi.rejectRequest(friendshipId)
            if (r.isSuccessful) Result.success(Unit) else Result.failure(Exception("Błąd odrzucenia (${r.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun mapFriendDto(dto: FriendshipDto): FriendItem? {
        val ref = dto.user ?: dto.userId ?: dto.friendId ?: return null
        val id = ref._id ?: dto.friendshipId ?: dto._id ?: return null
        val displayName = ref.profile?.displayName ?: ref.username ?: "(bez nazwy)"
        val username = ref.username ?: ""
        val avatar = ref.profile?.avatarUrl
        return FriendItem(id, displayName, username, avatar)
    }

    private suspend fun mapFriendshipList(status: String): Result<List<FriendItem>> = try {
        val response = RetrofitInstance.friendshipApi.getFriendships(status = status)
        if (response.isSuccessful) Result.success(response.body().orEmpty().mapNotNull { mapFriendDto(it) })
        else Result.failure(Exception("Nie udało się pobrać listy (${response.code()})"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun searchUsers(query: String): Result<List<UserSearchDto>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.userApi.searchUsers(query)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("Błąd wyszukiwania (${response.code()})"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendFriendRequest(recipientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Najpierw spróbuj friendId (częsty wariant w backendach), potem recipientId.
            val primary = RetrofitInstance.friendshipApi.sendRequest(FriendRequest(friendId = recipientId))
            if (primary.isSuccessful) return@withContext Result.success(Unit)

            // Jeśli 400/404 spróbuj alternatywne pole.
            val secondary = RetrofitInstance.friendshipApi.sendRequest(FriendRequest(recipientId = recipientId))
            if (secondary.isSuccessful) return@withContext Result.success(Unit)

            // Spróbuj wydobyć komunikat błędu.
            val raw = secondary.errorBody()?.string() ?: primary.errorBody()?.string()
            val msg = try {
                if (raw.isNullOrBlank()) null else JSONObject(raw).optString("message").takeIf { it.isNotBlank() }
            } catch (e: Exception) { null }
            Result.failure(Exception(msg ?: "Nie udało się wysłać zaproszenia (${secondary.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }
}