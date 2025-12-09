package com.example.projektiop.data.repositories

import android.util.Log
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.FriendRequest
import com.example.projektiop.data.db.objects.FriendshipStatus
import com.example.projektiop.data.mapping.toFriendItem
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.util.DataError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.Result

data class FriendItem(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val friendshipId: String,
    val blockedBy: String? = null
)

data class BlockInfo(
    val isBlocked: Boolean,
    val blockedByMe: Boolean,
    val friendshipId: String?
)

object FriendshipRepository {

    private val _friendsIds = MutableStateFlow<List<String>>(emptyList())
    val friendsIds: StateFlow<List<String>> = _friendsIds.asStateFlow()

    private val _blockedIds = MutableStateFlow<List<String>>(emptyList())
    val blockedIds : StateFlow<List<String>> = _blockedIds.asStateFlow()

    private val _pendingIds = MutableStateFlow<List<String>>(emptyList())
    val pendingIds : StateFlow<List<String>> = _pendingIds.asStateFlow()

    // private val _rejectedIds = MutableStateFlow<List<String>>(emptyList())
    // val rejectedIds : StateFlow<List<String>> = _rejectedIds.asStateFlow()
    suspend fun start() {
        fetchAccepted()
        fetchIncomingPending()
        fetchBlocked()
    }

    suspend fun fetchAccepted(): Result<List<FriendItem>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.friendshipApi.getFriendships()
            if (response.isSuccessful) {
                val all = response.body().orEmpty().filter { it.status == "accepted" }
                for (dto in all) {
                    runCatching {
                        DBRepository.addFriendship(dto.toRealm())
                        val tmpUser = dto.user?.toRealm()
                        if (tmpUser != null) DBRepository.addUser(tmpUser)
                    }.onFailure { e -> Result.failure<List<FriendItem>>(Exception("Failed to save to db", e))  }
                }
                _friendsIds.value = all.map{ it.user?._id.toString() }
                Result.success(all.mapNotNull { it.toFriendItem() })
            } else {
                val localAccepted = DBRepository.getFriendshipsByStatus(FriendshipStatus.ACCEPTED)
                if (localAccepted.isNotEmpty()) {
                    Result.success(localAccepted.map { it.toFriendItem() })
                }
                Result.failure(Exception("Nie udało się pobrać listy (${response.code()})"))
            }
        } catch (e: Exception) {
            val localAccepted = DBRepository.getFriendshipsByStatus(FriendshipStatus.ACCEPTED)
            if (localAccepted.isNotEmpty()) {
                Result.success(localAccepted.map { it.toFriendItem() })
            }
            Result.failure(e)
        }
    }

    suspend fun fetchIncomingPending(): Result<List<FriendItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitInstance.friendshipApi.getFriendships(status = "pending", direction = "incoming")
            if (!resp.isSuccessful)  throw(Exception("Błąd pobierania zaproszeń (${resp.code()})"))
            val items = resp.body().orEmpty().filter { it.isPendingRecipient == true || it.status == "pending" }
            for (dto in items) {
                runCatching {
                    DBRepository.addFriendship(dto.toRealm())
                    val tmpUser = dto.user?.toRealm()
                    if (tmpUser != null) DBRepository.addUser(tmpUser)
                    val tmpId = dto.user?._id.toString()
                }.onFailure { e -> Result.failure<List<FriendItem>>(Exception("Failed to save to db", e))  }
            }
            _pendingIds.value = items.map { it.user?._id.toString() }
            Result.success(items.mapNotNull { it.toFriendItem() })
        } catch (e: Exception) {
            val local = DBRepository.getFriendshipsByStatus(FriendshipStatus.PENDING) // TODO() direction
            if (local.isNotEmpty()) {
                Result.success(local.map { it.toFriendItem() })
            }
            Result.failure(e)
        }
    }

    suspend fun fetchBlocked(): Result<List<FriendItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitInstance.friendshipApi.getFriendships(status = "blocked")
            if (!resp.isSuccessful) throw(Exception("Api failed to fetch blocked (${resp.code()})"))
            val items = resp.body().orEmpty().filter { it.status == "blocked" }
            var tmpBlockedIds = emptyList<String>()
            for (dto in items) {
                runCatching {
                    DBRepository.addFriendship(dto.toRealm())
                    val tmpUser = dto.user?.toRealm()
                    if (tmpUser != null) DBRepository.addUser(tmpUser)
                    val tmpId = dto.user?._id.toString()
                    tmpBlockedIds += tmpId
                }.onFailure { e -> Result.failure<List<FriendItem>>(Exception("Failed to save to db", e))  }
                _blockedIds.value = tmpBlockedIds
            }
            Result.success(items.mapNotNull { it.toFriendItem() })
        } catch (e: Exception) {
            val local = DBRepository.getFriendshipsByStatus(FriendshipStatus.BLOCKED)
            if (local.isNotEmpty()) {
                Result.success(local.map { it.toFriendItem() })
            }
            Result.failure(e)
        }
    }

    suspend fun acceptFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitInstance.friendshipApi.acceptRequest(friendshipId)
            if (r.isSuccessful) {
                val friendId = DBRepository.getFriendshipById(friendshipId)!!.user2Id
                runCatching { DBRepository.changeFriendshipStatus(friendshipId, FriendshipStatus.ACCEPTED)
                }.onFailure {} //e -> Result.failure<List<FriendItem>>(Exception("Failed to save to db", e))  }
                _friendsIds.update { list ->
                    list + friendId.toString()
                }
                _pendingIds.update { list ->
                    list - friendId.toString()
                }
                Result.success(Unit)
            } else Result.failure(Exception("Błąd akceptacji (${r.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun rejectFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitInstance.friendshipApi.rejectRequest(friendshipId)
            if (r.isSuccessful) {
                val friendId = DBRepository.getFriendshipById(friendshipId)!!.user2Id
                runCatching { DBRepository.changeFriendshipStatus(friendshipId, FriendshipStatus.REJECTED)
                }.onFailure {} //e -> Result.failure<List<FriendItem>>(Exception("Failed to save to db", e))  }
                _pendingIds.update { list ->
                    list - friendId.toString()
                }
                Result.success(Unit)
            } else Result.failure(Exception("Błąd odrzucenia (${r.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }


    suspend fun sendFriendRequest(recipientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.friendshipApi.sendRequest(FriendRequest(recipientId = recipientId))
            if (response.isSuccessful) {
                return@withContext Result.success(Unit)
            }
            Result.failure(Exception("Nie udało się wysłać zaproszenia"))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("API error",e))
        }
    }

    suspend fun removeFriend(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitInstance.friendshipApi.removeFriendship(friendshipId)
            if (resp.isSuccessful) {
                runCatching { DBRepository.deleteFriendshipById(friendshipId) }
                Result.success(Unit)
            } else Result.failure(Exception("Błąd usuwania (${resp.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun blockFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitInstance.friendshipApi.blockFriendship(friendshipId)
            if (resp.isSuccessful) {
                val friendId = DBRepository.getFriendshipById(friendshipId)?.user2Id
                val blockedBy = resp.body()?.friendship?.blockedBy
                assert(blockedBy != null && friendId != null)
                DBRepository.blockFriendship(friendshipId, blockedBy!!)
                _blockedIds.update { list ->
                    list + friendId!!
                }
                _pendingIds.update { list ->
                    list - friendId!!
                }
                _friendsIds.update { list ->
                    list - friendId!!
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Błąd blokowania (${resp.code()})"))
            }
        } catch (e: Exception) {
            Log.d("block", "failed block $e")
            Result.failure(e)
        }
    }

    /*
    TODO() serverside, what does blocking a friendship mean?
        probably should be possible at any friendship state
        should just delete the friendship entry upon unblocking, since we don't save prior state
        implemented as is serverside right now
     */
    suspend fun unblockFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitInstance.friendshipApi.unblockFriendship(friendshipId)
            if (resp.isSuccessful) {
                val friendId = DBRepository.getFriendshipById(friendshipId)!!.user2Id
                DBRepository.unblockFriendship(friendshipId)
                _friendsIds.update { list ->
                    list + friendId.toString()
                }
                _blockedIds.update { list ->
                    list - friendId.toString()
                }
                Result.success(Unit)
            } else Result.failure(Exception("Błąd odblokowania (${resp.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getBlockInfo(friendId: String): Result<BlockInfo?> = withContext(Dispatchers.IO) {
        try {
            val myId = SharedPreferencesRepository.get("_id", "")
            val blocked = fetchBlocked().getOrNull().orEmpty()
            val target = blocked.firstOrNull { it.id == friendId }
            if (target == null) return@withContext Result.success(null)
            val blockedByMe = target.blockedBy == myId
            Result.success(BlockInfo(isBlocked = true, blockedByMe = blockedByMe, friendshipId = target.friendshipId))
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getLocalFriendItemByFriendId(friendId: String): com.example.projektiop.util.Result<FriendItem, DataError.Local> {
        try {
            val friendship = DBRepository.getFriendshipByFriendId(friendId)
            if (friendship == null) return com.example.projektiop.util.Result.Error(DataError.Local.NO_DATA)
            val friendItem = friendship.toFriendItem()
            return com.example.projektiop.util.Result.Success(friendItem)
        } catch(e: Exception) {
            return com.example.projektiop.util.Result.Error(DataError.Local.DB_ERROR)
        }
    }
}