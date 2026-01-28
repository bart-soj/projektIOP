package com.example.projektiop.data.repositories

import android.annotation.SuppressLint
import com.example.projektiop.data.api.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import com.example.projektiop.data.SharedDataSource
import com.example.projektiop.data.api.AccesChatRequest
import com.example.projektiop.data.api.ChatApi
import com.example.projektiop.data.api.ChatDto
import com.example.projektiop.data.api.MessagesPageDto
import com.example.projektiop.data.api.SendMessageRequest
import com.example.projektiop.domain.ChatEvent
import com.example.projektiop.data.api.websocket.SocketManager
import com.example.projektiop.data.db.realm.RealmDataSource
import com.example.projektiop.data.mapping.toDomain
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.domain.models.Message
import com.example.projektiop.domain.models.base64
import com.example.projektiop.data.util.KeyUtils
import com.example.projektiop.domain.DataError
import com.example.projektiop.util.NotificationHelper
import com.example.projektiop.data.util.apiExceptionToDataError
import com.example.projektiop.domain.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.bouncycastle.crypto.InvalidCipherTextException
import retrofit2.HttpException
import com.example.projektiop.domain.models.Chat as DomainChat
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject


private const val BASE_URL_KEY: String = "BASE_URL"


data class ChatListItem(
    val id: String,
    val title: String,
    val lastMessage: String,
    val lastMessageTime: String? = null,
    val friendId: String? = null,
    val avatarUrl: String? = null,
    val unread: Boolean = false
)

class ChatRepository(private val chatApi: ChatApi,
                     private val sharedDataSource: SharedDataSource,
                     private val dbRepository: RealmDataSource,
                     private val keyUtils: KeyUtils,
                     private val socketManager: SocketManager
) {

    private val _chats = MutableStateFlow<Flow<List<DomainChat>>>(flowOf(emptyList()))
    val chats = _chats.asStateFlow()

    private val KEY_ID = "_id"

    suspend fun fetchChats(myUserId: String): com.example.projektiop.domain.Result<List<DomainChat>, DataError> = withContext(Dispatchers.IO) {
        val dtoList: List<ChatDto>
        val domainList: MutableList<DomainChat> = emptyList<DomainChat>().toMutableList()
        try {
            val response = chatApi.getChats()
            if(!response.isSuccessful) throw HttpException(response)
            val body = response.body()
            require(body != null)
            dtoList = body

        } catch (e: Exception) {
            return@withContext apiExceptionToDataError<List<DomainChat>>(e)
        }

        dtoList.forEach {
            try {
                val chatId = it._id
                require(chatId != null)
                val otherUserId = it.participants?.firstOrNull { id -> id != myUserId }
                require(otherUserId != null)
                val otherUserRepository: OtherUserRepository by inject(OtherUserRepository::class.java) { parametersOf(otherUserId) }
                var otherUser: User? = otherUserRepository.Profile.value
                if (otherUser == null) {
                    val result = otherUserRepository.fetchProfile()
                    if (result.isSuccess) {
                        otherUser = result.getOrNull()
                    }
                }
                if (otherUser == null) throw Exception("otherUser not found")

                val chatKey: base64

                val domainChat: DomainChat
                val lastMessage = it.lastMessage
                var decrypted: String? = null
                if (lastMessage != null) {
                    if (lastMessage.content != null) {
                        chatKey = dbRepository.getChatById(chatId)?.chatKey
                            ?: run {
                                val pubResult = keyUtils.getPubKey(otherUserId)
                                if (pubResult !is com.example.projektiop.domain.Result.Success) throw Exception("No pubkey")
                                keyUtils.calculateChatKey(myUserId, pubResult.data)
                            }
                        try {
                            val content = lastMessage.content

                            decrypted = try {
                                keyUtils.decryptMessage(content, chatKey)
                            } catch (e: InvalidCipherTextException) {
                                val chatKeyResult = keyUtils.getPubKey(otherUserId)
                                if (chatKeyResult is com.example.projektiop.domain.Result.Success) {
                                    keyUtils.decryptMessage(content, chatKeyResult.data)
                                } else {
                                    throw e
                                }
                            }
                        } catch (e: Exception) { }

                        if (decrypted != null) {
                            val realmChat = it.toRealm(chatKey)
                            dbRepository.addChat(realmChat)
                            domainChat = realmChat.toDomain(
                                myUserId, dbRepository,
                                lastMessage.toDomain(decrypted)
                            )
                            domainList += domainChat
                        }
                    }
                } else {
                    val local = dbRepository.getChatById(chatId)?.chatKey
                    if (local != null) { chatKey = local
                    } else {
                        val otherUserPubResult = keyUtils.getPubKey(otherUserId)
                        val otherUserPub: base64?
                        when (otherUserPubResult) {
                            is com.example.projektiop.domain.Result.Error -> {
                                return@forEach
                            }

                            is com.example.projektiop.domain.Result.Success -> otherUserPub =
                                otherUserPubResult.data
                        }
                        chatKey = keyUtils.calculateChatKey(myUserId, otherUserPub)
                    }
                    val realmChat = it.toRealm(chatKey)
                    dbRepository.addChat(realmChat)
                    domainChat = realmChat.toDomain(myUserId, dbRepository)
                    domainList += domainChat
                }
            } catch(e: Exception ) {
                Log.d("CHATS", "failed to resolve chat $e")
            }
        }
        return@withContext com.example.projektiop.domain.Result.Success(domainList)
    }

    suspend fun ensureChatWithUser(friendId: String, myId: String): com.example.projektiop.domain.Result<DomainChat, DataError>  {
        val localData = dbRepository.getChatByFriendId(friendId)
        if (localData != null) return com.example.projektiop.domain.Result.Success(localData.toDomain(myId, dbRepository))
        try {
            // if we created the chat and got an error before saving it could exist on server but not locally
            var dto: ChatDto? = null
            val existing = chatApi.getChats()
            if (existing.isSuccessful) {
                existing.body().orEmpty().firstOrNull { chat ->
                    chat.participants?.any { it == friendId } == true
                }?.let {
                    dto = it
                }
            }
            if (dto == null){
                val created = chatApi.accessChat(AccesChatRequest(friendId))
                if(!created.isSuccessful) throw HttpException(created)
                dto = created.body()
            }

            require(dto != null)

            val otherUserPubResult = keyUtils.getPubKey(friendId)
            val otherUserPub: base64?
            when (otherUserPubResult) {
                is com.example.projektiop.domain.Result.Error -> { return com.example.projektiop.domain.Result.Error(otherUserPubResult.error) }
                is com.example.projektiop.domain.Result.Success -> otherUserPub =
                    otherUserPubResult.data
            }
            val chatKey = keyUtils.calculateChatKey(myId, otherUserPub)
            val realmChat = dto.toRealm(chatKey)
            dbRepository.addChat(realmChat)
            val domainChat = realmChat.toDomain(myId, dbRepository)
            return com.example.projektiop.domain.Result.Success(domainChat)
        } catch (e: Exception) {
            return apiExceptionToDataError<DomainChat>(e)
        }
    }

    suspend fun loadMessages(chatId: String, friendId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        var r: retrofit2.Response<MessagesPageDto>
        try {
            r = chatApi.getMessages(chatId)
            if (!r.isSuccessful) {
                throw HttpException(r)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        var chatKey = dbRepository.getChatById(chatId)!!.chatKey
        val messages = r.body()?.messages
        if (messages.orEmpty() == emptyList<MessageDto>()) return@withContext Result.success(emptyList())
        val firstMessage = messages?.first()

        // if we fail to decrypt the message with locally saved key we try to get a new one
        try {
            val decrypted = keyUtils.decryptMessage(firstMessage!!.content!!, chatKey!!)
        } catch (e: InvalidCipherTextException) {
            val chatKeyResult = keyUtils.getPubKey(friendId)
            when(chatKeyResult) {
                is com.example.projektiop.domain.Result.Error -> {throw e}
                is com.example.projektiop.domain.Result.Success -> { chatKey = chatKeyResult.data }
            }
        }

        try {
            return@withContext Result.success(messages!!.map {
                require(it.content != null)
                val decrypted = keyUtils.decryptMessage(it.content, chatKey)
                it.toDomain(decrypted)
            })
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("failed to load messages $e"))
        }
    }

    suspend fun decryptMessage(message: Message, friendId: String): Result<Message> {
        return try {
            val chatKey = dbRepository.getChatById(message.chatId)?.chatKey

            val decrypted = try {
                keyUtils.decryptMessage(message.content, chatKey!!)
            } catch (e: InvalidCipherTextException) {
                val chatKeyResult = keyUtils.getPubKey(friendId)
                if (chatKeyResult is com.example.projektiop.domain.Result.Success) {
                    keyUtils.decryptMessage(message.content, chatKeyResult.data)
                } else {
                    throw e
                }
            }

            val out = Message(
                id = message.id,
                chatId = message.chatId,
                content = decrypted,
                readBy = message.readBy,
                senderId = message.senderId,
                createdAt = message.createdAt
            )
            return Result.success(out)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, content: String): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val chatKey = dbRepository.getChatById(chatId)!!.chatKey
            val encrypted = keyUtils.encryptMessage(content, chatKey!!)
            val r = chatApi.sendMessage(SendMessageRequest(encrypted, chatId))
            if (r.isSuccessful) Result.success(r.body()!!.toDomain(encrypted)) else Result.failure(Exception("Błąd wysyłania (${r.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendMessageSocket(chatId: String, content: String): Result<Unit> {
        try {
            val chatKey = dbRepository.getChatById(chatId)!!.chatKey
            val encrypted = keyUtils.encryptMessage(content, chatKey!!)
            socketManager.emit(ChatEvent.Send(chatId, encrypted))
            return Result.success(Unit)
        } catch (e: Exception) { return Result.failure(e) }
    }

    fun connectToService(serviceFlow: StateFlow<List<DomainChat>>) {
        _chats.value = serviceFlow
    }

    @SuppressLint("MissingPermission")
    suspend fun receiveMessage(message: Message, context: Context) {
        try {
            val myId = sharedDataSource.get(KEY_ID, "")
            val chatId = message.chatId
            val chat = dbRepository.getChatById(chatId)
            val friendId = chat?.participants?.first{myId != it}
            val friend = dbRepository.getUserById(friendId!!)
            decryptMessage(message, friendId).onSuccess { decrypted ->
                dbRepository.updateLastMessage(chatId, message.id, message.createdAt.toString())
                if (message.senderId != myId) {
                    NotificationHelper.notifyMessage(context, friend?.username.toString(), decrypted.content.take(100))
                }
            }
        } catch (e: Exception) {}
    }
}
