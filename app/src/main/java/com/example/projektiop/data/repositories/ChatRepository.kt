package com.example.projektiop.data.repositories

import com.example.projektiop.data.api.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import com.example.projektiop.data.api.AccesChatRequest
import com.example.projektiop.data.api.ChatApi
import com.example.projektiop.data.api.ChatDto
import com.example.projektiop.data.api.SendMessageRequest
import com.example.projektiop.data.api.websocket.ChatEvent
import com.example.projektiop.data.api.websocket.SocketManager
import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.data.mapping.toDomain
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.mapping.toUserProfileResponse
import com.example.projektiop.domain.models.Message
import com.example.projektiop.domain.models.base64
import com.example.projektiop.util.CertificateUtils
import com.example.projektiop.util.DataError
import com.example.projektiop.util.apiExceptionToDataError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import com.example.projektiop.domain.models.Chat as DomainChat

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
                     context: Context,
                     private val sharedDataSource: SharedDataSource,
                     private val dbRepository: RealmDBRepository,
                     private val certificateUtils: CertificateUtils
) {

    private val _chats = MutableStateFlow<Flow<List<DomainChat>>>(flowOf(emptyList()))
    val chats = _chats.asStateFlow()

    private fun normalizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val base = sharedDataSource.get(BASE_URL_KEY,"")
        return if (trimmed.startsWith("/")) base + trimmed else "$base/$trimmed"
    }


    suspend fun fetchChats(myUserId: String): com.example.projektiop.util.Result<List<DomainChat>, DataError> = withContext(Dispatchers.IO) {
        val dtoList: List<ChatDto>
        var domainList: List<DomainChat> = emptyList()
        try {
            val response = chatApi.getChats()
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
                val otherUser = dbRepository.getUserById(otherUserId)
                require(otherUser != null) // should have users we have chats with since they are friends

                val local = dbRepository.getChatById( chatId )?.chatKey
                val chatKey: base64
                if (! local.isNullOrBlank()) {
                    chatKey = local
                } else {
                    val otherUserPubResult = certificateUtils.getPubKey(otherUserId)
                    val otherUserPub: base64?
                    when (otherUserPubResult) {
                        is com.example.projektiop.util.Result.Error -> { return@forEach }

                        is com.example.projektiop.util.Result.Success -> otherUserPub =
                            otherUserPubResult.data
                    }
                    chatKey = certificateUtils.calculateChatKey(myUserId, otherUserPub)
                }

                val realmChat = it.toRealm(chatKey)
                dbRepository.addChat(realmChat)
                val domainChat: DomainChat
                val theirPubResult = certificateUtils.getPubKey(otherUserId)
                when(theirPubResult) {
                    is com.example.projektiop.util.Result.Error -> {
                    domainChat = realmChat.toDomain(myUserId, dbRepository)}
                    is com.example.projektiop.util.Result.Success -> {
                        if (it.lastMessage != null) {
                            val chatKey = certificateUtils.calculateChatKey(myUserId, theirPubResult.data)
                            val decrypted = certificateUtils.decryptMessage(it.lastMessage.content!!, chatKey)
                            domainChat = realmChat.toDomain(myUserId, dbRepository,
                                it.lastMessage.toDomain(decrypted)
                            )
                        } else {
                            domainChat = realmChat.toDomain(myUserId, dbRepository)
                        }
                    }
                }
                domainList += domainChat
            } catch(e: Exception ) {}
        }

        return@withContext com.example.projektiop.util.Result.Success(domainList)
    }

    suspend fun ensureChatWithUser(friendId: String, myId: String): com.example.projektiop.util.Result<DomainChat, DataError>  {
        val localData = dbRepository.getChatByFriendId(friendId)
        if (localData != null) return com.example.projektiop.util.Result.Success(localData.toDomain(myId, dbRepository))
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
                dto = created.body()
            }

            require(dto != null)

            val otherUserPubResult = certificateUtils.getPubKey(friendId)
            val otherUserPub: base64?
            when (otherUserPubResult) {
                is com.example.projektiop.util.Result.Error -> { return com.example.projektiop.util.Result.Error(otherUserPubResult.error) }
                is com.example.projektiop.util.Result.Success -> otherUserPub =
                    otherUserPubResult.data
            }
            val chatKey = certificateUtils.calculateChatKey(myId, otherUserPub)
            val realmChat = dto.toRealm(chatKey)
            dbRepository.addChat(realmChat)
            val domainChat = realmChat.toDomain(myId, dbRepository)
            return com.example.projektiop.util.Result.Success(domainChat)
        } catch (e: Exception) {
            return apiExceptionToDataError<DomainChat>(e)
        }
    }

    suspend fun loadMessages(chatId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        try {
            val r = chatApi.getMessages(chatId)
            val chatKey = dbRepository.getChatById(chatId)!!.chatKey
            if (r.isSuccessful) {
                return@withContext Result.success(r.body()?.messages.orEmpty().map {
                    require(it.content != null)
                    val decrypted = certificateUtils.decryptMessage(it.content, chatKey!!)
                    it.toDomain(decrypted)
                })
            } else {
                return@withContext Result.failure(Exception("Błąd pobierania wiadomości (${r.code()})"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun decryptMessage(message: Message): Result<Message> {
        try {
            val chatKey = dbRepository.getChatById(message.chatId)!!.chatKey
            val decrypted = certificateUtils.decryptMessage(message.content, chatKey!!)
            val out = Message(
                id = message.id,
                chatId = message.chatId,
                content = decrypted,
                readBy = message.readBy,
                senderId = message.senderId,
                createdAt = message.createdAt
            )
            return Result.success(out)
        } catch (e: Exception) { return Result.failure(e) }
    }

    suspend fun sendMessage(chatId: String, content: String): Result<MessageDto> = withContext(Dispatchers.IO) {
        try {
            val chatKey = dbRepository.getChatById(chatId)!!.chatKey
            val encrypted = certificateUtils.encryptMessage(content, chatKey!!)
            val r = chatApi.sendMessage(SendMessageRequest(encrypted, chatId))
            if (r.isSuccessful) Result.success(r.body()!!) else Result.failure(Exception("Błąd wysyłania (${r.code()})"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendMessageSocket(chatId: String, content: String, socketManager: SocketManager): Result<Unit> {
        try {
            val chatKey = dbRepository.getChatById(chatId)!!.chatKey
            val encrypted = certificateUtils.encryptMessage(content, chatKey!!)
            socketManager.emit(ChatEvent.Send(chatId, encrypted))
            return Result.success(Unit)
        } catch (e: Exception) { return Result.failure(e) }
    }

    fun connectToService(serviceFlow: StateFlow<List<DomainChat>>) {
        _chats.value = serviceFlow
    }
}
