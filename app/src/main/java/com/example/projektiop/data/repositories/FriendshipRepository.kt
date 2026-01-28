package com.example.projektiop.data.repositories

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.projektiop.data.SharedDataSource
import com.example.projektiop.data.api.FriendRequest
import com.example.projektiop.data.api.FriendshipApi
import com.example.projektiop.data.db.realm.RealmDataSource
import com.example.projektiop.data.mapping.toDomain
import com.example.projektiop.data.mapping.toFriendItem
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.domain.models.Friendship
import com.example.projektiop.domain.models.FriendshipStatus
import com.example.projektiop.domain.DataError
import com.example.projektiop.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import kotlin.Result
import kotlin.getValue

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
)

class FriendshipRepository(private val friendshipApi: FriendshipApi,
                           private val databaseDataSource: RealmDataSource,
                           private val sharedDataSource: SharedDataSource) {

    private val _friendsIds = MutableStateFlow<List<String>>(emptyList())
    val friendsIds: StateFlow<List<String>> = _friendsIds.asStateFlow()

    private val _blockedIds = MutableStateFlow<List<String>>(emptyList())
    val blockedIds : StateFlow<List<String>> = _blockedIds.asStateFlow()

    private val _pendingIds = MutableStateFlow<List<String>>(emptyList())
    val pendingIds : StateFlow<List<String>> = _pendingIds.asStateFlow()

    // private val _rejectedIds = MutableStateFlow<List<String>>(emptyList())
    // val rejectedIds : StateFlow<List<String>> = _rejectedIds.asStateFlow()

    suspend fun refreshAll() {
        fetchAccepted()
        fetchIncomingPending()
        fetchBlocked()
    }

    suspend fun fetchAccepted(): Result<List<FriendItem>> = withContext(Dispatchers.IO) {
        try {
            val response = friendshipApi.getFriendships()
            if (response.isSuccessful) {
                val all = response.body().orEmpty().filter { it.status == "accepted" }
                for (dto in all) {
                    runCatching {
                        val myId = sharedDataSource.get("_id", "")
                        assert(myId.isNotBlank())
                        databaseDataSource.addFriendship(dto.toRealm(myId))
                        val tmpUser = dto.user?.toRealm()
                        if (tmpUser != null) databaseDataSource.addUser(tmpUser)
                    }.onFailure { e -> Result.failure<List<FriendItem>>(Exception("Failed to save to db", e))  }
                }
                _friendsIds.value = all.map{ it.user?._id.toString() }
                Result.success(all.mapNotNull { it.toFriendItem() })
            } else {
                val localAccepted = databaseDataSource.getFriendshipsByStatus(FriendshipStatus.ACCEPTED)
                if (localAccepted.isNotEmpty()) {
                    Result.success(localAccepted.map { it.toFriendItem(databaseDataSource.getUserById(it._id.toHexString())) })
                }
                Result.failure(Exception("Nie udało się pobrać listy (${response.code()})"))
            }
        } catch (e: Exception) {
            val localAccepted = databaseDataSource.getFriendshipsByStatus(FriendshipStatus.ACCEPTED)
            if (localAccepted.isNotEmpty()) {
                Result.success(localAccepted.map { it.toFriendItem(databaseDataSource.getUserById(it._id.toHexString())) })
            }
            Result.failure(e)
        }
    }

    suspend fun fetchIncomingPending(): Result<List<FriendItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = friendshipApi.getFriendships(status = "pending", direction = "incoming")
            if (!resp.isSuccessful)  throw(Exception("Błąd pobierania zaproszeń (${resp.code()})"))
            val items = resp.body().orEmpty().filter { it.isPendingRecipient == true || it.status == "pending" }
            for (dto in items) {
                runCatching {
                    databaseDataSource.addFriendship(dto.toRealm(sharedDataSource.get("_id", "")))
                    val tmpUser = dto.user?.toRealm()
                    if (tmpUser != null) databaseDataSource.addUser(tmpUser)
                    val tmpId = dto.user?._id.toString()
                }.onFailure { e -> Result.failure<List<FriendItem>>(Exception("Failed to save to db", e))  }
            }
            _pendingIds.value = items.map { it.user?._id.toString() }
            Result.success(items.mapNotNull { it.toFriendItem() })
        } catch (e: Exception) {
            val local = databaseDataSource.getFriendshipsByStatus(FriendshipStatus.PENDING) // TODO() direction
            if (local.isNotEmpty()) {
                Result.success(local.map { it.toFriendItem(databaseDataSource.getUserById(it._id.toHexString())) })
            }
            Result.failure(e)
        }
    }

    suspend fun fetchBlocked(): Result<List<FriendItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = friendshipApi.getFriendships(status = "blocked")
            if (!resp.isSuccessful) throw(Exception("Api failed to fetch blocked (${resp.code()})"))
            val items = resp.body().orEmpty().filter { it.status == "blocked" }
            var tmpBlockedIds = emptyList<String>()
            for (dto in items) {
                runCatching {
                    databaseDataSource.addFriendship(dto.toRealm(sharedDataSource.get("_id", "")))
                    val tmpUser = dto.user?.toRealm()
                    if (tmpUser != null) databaseDataSource.addUser(tmpUser)
                    val tmpId = dto.user?._id.toString()
                    tmpBlockedIds += tmpId
                }.onFailure { e -> Result.failure<List<FriendItem>>(Exception("Failed to save to db", e))  }
                _blockedIds.value = tmpBlockedIds
            }
            Result.success(items.mapNotNull { it.toFriendItem() })
        } catch (e: Exception) {
            val local = databaseDataSource.getFriendshipsByStatus(FriendshipStatus.BLOCKED)
            if (local.isNotEmpty()) {
                Result.success(local.map { it.toFriendItem(databaseDataSource.getUserById(it._id.toHexString())) })
            }
            Result.failure(e)
        }
    }

    suspend fun acceptFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val r = friendshipApi.acceptRequest(friendshipId)
            if (r.isSuccessful) {
                val friendId = databaseDataSource.getFriendshipById(friendshipId)!!.user2Id
                runCatching { databaseDataSource.changeFriendshipStatus(friendshipId, FriendshipStatus.ACCEPTED)
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
            val r = friendshipApi.rejectRequest(friendshipId)
            if (r.isSuccessful) {
                val friendId = databaseDataSource.getFriendshipById(friendshipId)!!.user2Id
                runCatching { databaseDataSource.changeFriendshipStatus(friendshipId, FriendshipStatus.REJECTED)
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
            val response = friendshipApi.sendRequest(FriendRequest(recipientId = recipientId))
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
            val resp = friendshipApi.removeFriendship(friendshipId)
            if (resp.isSuccessful) {
                runCatching { databaseDataSource.deleteFriendshipById(friendshipId) }
                Result.success(Unit)
            } else Result.failure(Exception("Błąd usuwania (${resp.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun blockFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = friendshipApi.blockFriendship(friendshipId)
            if (resp.isSuccessful) {
                val friendId = databaseDataSource.getFriendshipById(friendshipId)?.user2Id
                val blockedBy = resp.body()?.friendship?.blockedBy
                assert(blockedBy != null && friendId != null)
                databaseDataSource.blockFriendship(friendshipId, blockedBy!!)
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

    suspend fun getBlocked(friendId: String) {
        databaseDataSource.getBlocked(friendId)
    }

    suspend fun unblockFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = friendshipApi.unblockFriendship(friendshipId)
            if (resp.isSuccessful) {
                val friendId = databaseDataSource.getFriendshipById(friendshipId)!!.user2Id
                databaseDataSource.unblockFriendship(friendshipId)
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

    suspend fun getUnblocked(friendId: String) {
        databaseDataSource.getUnblocked(friendId)
    }

    fun getLocalFriendItemByFriendId(friendId: String): com.example.projektiop.domain.Result<FriendItem, DataError.Local> {
        try {
            val friendship = databaseDataSource.getFriendshipByFriendId(friendId)
            if (friendship == null) return com.example.projektiop.domain.Result.Error(DataError.Local.NO_DATA)
            val friendItem = friendship.toFriendItem(databaseDataSource.getUserById(friendId))
            return com.example.projektiop.domain.Result.Success(friendItem)
        } catch(e: Exception) {
            return com.example.projektiop.domain.Result.Error(DataError.Local.DB_ERROR)
        }
    }

    fun getLocalFriendship(friendId: String): com.example.projektiop.domain.Result<Friendship, DataError.Local> {
        try {
            val friendship = databaseDataSource.getFriendshipByFriendId(friendId)
            val friend = databaseDataSource.getUserById(friendId)?.toDomain()
            if (friendship == null || friend == null) return com.example.projektiop.domain.Result.Error(DataError.Local.NO_DATA)
            val friendItem = friendship.toDomain(friend)
            return com.example.projektiop.domain.Result.Success(friendItem)
        } catch(e: Exception) {
            return com.example.projektiop.domain.Result.Error(DataError.Local.DB_ERROR)
        }
    }

    suspend fun onInviteAccepted(friendshipId: String, appContext: Context) {
        val friendId = databaseDataSource.getFriendshipById(friendshipId)!!.user2Id
        databaseDataSource.changeFriendshipStatus(friendshipId, FriendshipStatus.ACCEPTED)
        _pendingIds.update { list ->
            list - friendId.toString()
        }
        _friendsIds.update { list ->
            list + friendId.toString()
        }
        val otherUserRepository: OtherUserRepository by inject(OtherUserRepository::class.java) { parametersOf(friendId) }
        otherUserRepository.fetchProfile()
        val displayName = otherUserRepository.Profile.value?.profile?.displayName ?:
        otherUserRepository.Profile.value?.username
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationHelper.notifyFriendshipStatus(appContext, displayName.toString(), FriendshipStatus.ACCEPTED)
    }

    suspend fun onInviteRejected(friendshipId: String, appContext: Context ) {
        val friendId = databaseDataSource.getFriendshipById(friendshipId)!!.user2Id
        databaseDataSource.changeFriendshipStatus(friendshipId, FriendshipStatus.REJECTED)
        _pendingIds.update { list ->
            list - friendId.toString()
        }
        val otherUserRepository: OtherUserRepository by inject(OtherUserRepository::class.java) { parametersOf(friendId) }
        otherUserRepository.fetchProfile()
        val displayName = otherUserRepository.Profile.value?.profile?.displayName ?:
        otherUserRepository.Profile.value?.username
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationHelper.notifyFriendshipStatus(appContext, displayName.toString(), FriendshipStatus.REJECTED)
    }

    suspend fun onFriendshipEnded(friendshipId: String, appContext: Context ) {
        val friendId = databaseDataSource.getFriendshipById(friendshipId)!!.user2Id
        databaseDataSource.changeFriendshipStatus(friendshipId, FriendshipStatus.NOT_FRIENDS)
        _pendingIds.update { list ->
            list - friendId.toString()
        }
        val otherUserRepository: OtherUserRepository by inject(OtherUserRepository::class.java) { parametersOf(friendId) }
        otherUserRepository.fetchProfile()
        val displayName = otherUserRepository.Profile.value?.profile?.displayName ?:
        otherUserRepository.Profile.value?.username
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationHelper.notifyFriendshipStatus(appContext, displayName.toString(), FriendshipStatus.NOT_FRIENDS)
    }
}