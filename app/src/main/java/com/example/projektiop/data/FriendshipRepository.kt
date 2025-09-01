package com.example.projektiop.data

import com.example.projektiop.api.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FriendItem(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?
)

object FriendshipRepository {
    suspend fun fetchAccepted(): Result<List<FriendItem>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.friendshipApi.getFriendships(status = "accepted")
            if (response.isSuccessful) {
                val list = response.body().orEmpty().mapNotNull { dto ->
                    // Wybierz drugą stronę przyjaźni (userId lub friendId) – oba mogą wystąpić
                    val ref = dto.userId ?: dto.friendId
                    val id = ref?._id ?: dto._id ?: return@mapNotNull null
                    val displayName = ref?.profile?.displayName ?: ref?.username ?: "(bez nazwy)"
                    val username = ref?.username ?: "" // do ewent. czatu
                    val avatar = ref?.profile?.avatarUrl
                    FriendItem(id = id, displayName = displayName, username = username, avatarUrl = avatar)
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Nie udało się pobrać znajomych (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}