package com.example.projektiop.data.db.realm

import com.example.projektiop.data.db.realm.objects.Chat
import com.example.projektiop.data.db.realm.objects.Friendship
import com.example.projektiop.data.db.realm.objects.Interest
import com.example.projektiop.data.db.realm.objects.InterestCategory
import com.example.projektiop.data.db.realm.objects.Message
import com.example.projektiop.data.db.realm.objects.SearchProfile
import com.example.projektiop.data.db.realm.objects.User
import com.example.projektiop.data.db.realm.objects.UserInterest
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.map
import com.example.projektiop.domain.models.FriendshipStatus
import com.example.projektiop.domain.models.base64
import com.example.projektiop.util.mongoTimestampToRealmInstant
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId
import java.time.Instant
import com.example.projektiop.domain.models.SearchProfile as DomainSearchProfile

class RealmDBRepository(private val realm: Realm) {

    suspend fun deleteContents() {
        realm.write{
            this.deleteAll()
        }
    }

    // ----------------------
    // Message operations
    // ----------------------
    fun getMessages(): List<Message> {
        return realm.query<Message>(Message::class).find()
    }

    fun getMessagesByChat(chatId: String): List<Message> {
        val objectId = ObjectId(chatId)
        return realm.query<Message>(Message::class, "chatId == $0", objectId).find()
    }

    fun getMessageById(messageId: String): Message? {
        val objectId = ObjectId(messageId)
        return realm.query<Message>(Message::class, "_id = $0", objectId).first().find()
    }

    fun addMessage(message: Message) {
        realm.writeBlocking {
            copyToRealm(message, updatePolicy = UpdatePolicy.ALL)
        }
    }

    fun updateMessage(message: Message) {
        realm.writeBlocking {
            findLatest(message)?.let {
                it.content = message.content
                it.readBy = message.readBy
                it.updatedAt = message.updatedAt
            }
        }
    }

    fun deleteMessage(message: Message) {
        realm.writeBlocking {
            findLatest(message)?.let { delete(it) }
        }
    }

    // ----------------------
    // CHat operations
    // ----------------------

    fun addChat(toAdd: Chat) {
        realm.writeBlocking {
            copyToRealm(toAdd, updatePolicy = UpdatePolicy.ALL )
        }
    }

    fun getChatByFriendId(friendId: String): Chat? {
        return realm.query<Chat>(Chat::class, "participants = $0", friendId).first().find()
    }

    fun getChatById(chatId: String): Chat? {
        val objectId = ObjectId(chatId)
        return realm.query<Chat>(Chat::class, "_id = $0", objectId).first().find()
    }

    fun updateLastMessage(chatId: String, lastMessageId: String, lastMessageTimestamp: String) {
        realm.writeBlocking {
            val objectId = ObjectId(chatId)
            val toUpdate = this.query<Chat>(Chat::class, "_id = $0", objectId).first().find()
            toUpdate?.lastMessageId = lastMessageId
            toUpdate?.lastMessageTimestamp = mongoTimestampToRealmInstant(lastMessageTimestamp)
            toUpdate?.updatedAt = RealmInstant.now()
        }
    }

    fun deleteChat(toDelete: Chat) {
        realm.writeBlocking {
            findLatest(toDelete)?.let { delete(it) }
        }
    }

    // ----------------------
    // User operations
    // ----------------------
    fun getUsers(): List<User> {
        return realm.query<User>(User::class).find()
    }

    fun getUserById(userId: String): User? {
        val objectId = ObjectId(userId)
        return realm.query<User>(User::class, "_id == $0", objectId).first().find()
    }

    fun getUserByEmail(email: String): User? {
        return realm.query<User>(User::class, "email == $0", email).first().find()
    }

    fun addUser(user: User) {
        realm.writeBlocking {
            copyToRealm(user, updatePolicy = UpdatePolicy.ALL)
        }
    }

    fun updateUser(user: User) {
        realm.writeBlocking {
            findLatest(user)?.let {
                it.username = user.username
                it.email = user.email
                it.profile = user.profile
                it.role = user.role
                it.isBanned = user.isBanned
                it.updatedAt = user.updatedAt
            }
        }
    }

    fun savePubKey(userId: String, pubKey: base64) {
        val objectId = ObjectId(userId)
        realm.writeBlocking {
            val user = this.query<User>(User::class, "_id == $0", objectId).first().find()
            if (user != null) {
                user.publicKey = pubKey
            }
        }
    }

    fun deleteUser(user: User) {
        realm.writeBlocking {
            findLatest(user)?.let { delete(it) }
        }
    }

    fun getUserFlowById(_id: String): Flow<User?> {
        return realm.query<User>(User::class, "_id == $0", ObjectId(_id))
            .asFlow()
            .cancellable()
            .map { changeList  ->
                changeList.list.firstOrNull()
            }
    }

    // ----------------------
    // Friendship operations
    // ----------------------
    fun getFriendships(): List<Friendship> {
        return realm.query<Friendship>(Friendship::class).find()
    }

    fun getFriendshipById(friendshipId: String): Friendship? {
        val objectId = ObjectId(friendshipId)
        return realm.query<Friendship>(Friendship::class, "_id == $0", objectId).first().find()
    }

    fun getFriendshipByFriendId(friendId: String): Friendship? {
        return realm.query<Friendship>(Friendship::class, "user2Id == $0", friendId).first().find()
    }

    fun getFriendshipsByStatus(status: FriendshipStatus): List<Friendship> {
        return realm.query<Friendship>(Friendship::class, "_status == $0", status.name).find()
    }

    fun addFriendship(friendship: Friendship) {
        realm.writeBlocking {
            copyToRealm(friendship, updatePolicy = UpdatePolicy.ALL)
        }
    }

    fun changeFriendshipStatus(friendshipId: String, status: FriendshipStatus) {
        realm.writeBlocking {
            val objectId = ObjectId(friendshipId)
            val toUpdate = this.query<Friendship>(Friendship::class, "_id == $0", objectId).first().find()
            if (toUpdate == null) return@writeBlocking
            toUpdate.status = status
            if (status == FriendshipStatus.BLOCKED) {
                toUpdate.isBlocked = true
            }
        }
    }



    fun blockFriendship(friendshipId: String, blockedBy: String) {
        realm.writeBlocking {
            val objectId = ObjectId(friendshipId)
            val toUpdate = this.query<Friendship>(Friendship::class, "_id == $0", objectId).first().find()
            if (toUpdate == null) return@writeBlocking
            toUpdate.status = FriendshipStatus.BLOCKED
            toUpdate.isBlocked = true
            toUpdate.blockedBy = blockedBy
        }
    }

    fun unblockFriendship(friendshipId: String) {
        realm.writeBlocking {
            val objectId = ObjectId(friendshipId)
            val toUpdate = this.query<Friendship>(Friendship::class, "_id == $0", objectId).first().find()
            if (toUpdate == null) return@writeBlocking
            toUpdate.status = FriendshipStatus.ACCEPTED
            toUpdate.isBlocked = false
            toUpdate.blockedBy = null
        }
    }

    fun getBlocked(friendId: String) {
        realm.writeBlocking {
            val toUpdate = this.query<Friendship>(Friendship::class, "user2Id == $0", friendId).first().find()
            if (toUpdate == null) return@writeBlocking
            toUpdate.status = FriendshipStatus.BLOCKED
            toUpdate.isBlocked = true
            toUpdate.blockedBy = friendId
        }
    }

    fun getUnblocked(friendId: String) {
        realm.writeBlocking {
            val toUpdate = this.query<Friendship>(Friendship::class, "user2Id == $0", friendId).first().find()
            if (toUpdate == null) return@writeBlocking
            toUpdate.status = FriendshipStatus.ACCEPTED
            toUpdate.isBlocked = false
            toUpdate.blockedBy = null
        }
    }

    fun deleteFriendshipById(friendshipId: String) {
        val objectId = ObjectId(friendshipId)
        val friendship = realm.query<Friendship>(Friendship::class, "_id == $0", objectId)
            .first().find() ?: throw Exception("can't delete, no such friendship")

        realm.writeBlocking {
            findLatest(friendship)?.let { delete(it) }
        }
    }

    fun deleteFriendship(friendship: Friendship) {
        realm.writeBlocking {
            findLatest(friendship)?.let { delete(it) }
        }
    }

    // ----------------------
    // UserInterest operations
    // ----------------------
    fun getUserInterestsByUserId(userId: String): List<UserInterest> {
        val objectId = ObjectId(userId)
        return realm.query<UserInterest>(UserInterest::class, "userId == $0", objectId).find()
    }

    fun addUserInterest(userInterest: UserInterest) {
        realm.writeBlocking {
            val user = query<User>(User::class, "_id == $0", userInterest.userId).first().find()
            val interest = query<Interest>(Interest::class, "_id == $0", userInterest.interestId).first().find()

            if (user != null && interest != null) {
                copyToRealm(userInterest.apply {
                    this.interest = interest
                }, updatePolicy = UpdatePolicy.ALL)

                user.interests.add(userInterest)
            } else {
                throw Exception("No such user or interest")
            }
        }
    }

    fun updateUserInterest(userInterest: UserInterest) {
        realm.writeBlocking {
            findLatest(userInterest)?.let {
                it.customDescription = userInterest.customDescription
                it.updatedAt = userInterest.updatedAt
            }
        }
    }

    fun deleteUserInterest(userInterest: UserInterest) {
        realm.writeBlocking {
            findLatest(userInterest)?.let { delete(it) }
        }
    }

    // ----------------------
    // Interest operations
    // ----------------------
    fun addInterest(interest: Interest) {
        realm.writeBlocking {
            val category = query<InterestCategory>(InterestCategory::class, "_id == $0", interest.categoryId).first().find()
            if (category != null) {
                interest.apply {
                    this.category = category
                }
                copyToRealm(interest, updatePolicy = UpdatePolicy.ALL)
            } else {
                throw Exception("No such category")
            }
        }
    }

    fun getInterestById(id: String): Interest? {
        val objectId = ObjectId(id)
        return realm.query<Interest>(Interest::class, "_id == $0", objectId).first().find()
    }

    fun getInterestByName(name: String): Interest? {
        return realm.query<Interest>(Interest::class, "name == $0", name).first().find()
    }

    fun getInterests(): List<Interest>? {
        return realm.query<Interest>(Interest::class).find()
    }

    fun deleteInterest(interest: Interest) {
        realm.writeBlocking {
            findLatest(interest)?.let { delete(it) }
        }
    }

    fun addInterestPair(interest: Interest, userInterest: UserInterest) {
        realm.writeBlocking{
            // add interest
            val category = query<InterestCategory>(InterestCategory::class, "_id == $0", interest.categoryId).first().find()
            if (category != null) {
                interest.apply {
                    this.category = category
                }
                copyToRealm(interest, updatePolicy = UpdatePolicy.ALL)
            } else {
                throw Exception("No such category")
            }

            // add user interest
            val user = query<User>(User::class, "_id == $0", userInterest.userId).first().find()
            val interest = query<Interest>(Interest::class, "_id == $0", userInterest.interestId).first().find()

            if (user != null && interest != null) {
                copyToRealm(userInterest.apply {
                    this.interest = interest
                }, updatePolicy = UpdatePolicy.ALL)

                user.interests.add(userInterest)
            } else {
                throw Exception("No such user or interest")
            }

        }
    }

    // ----------------------
    // InterestCategory operations
    // ----------------------
    fun addInterestCategory(interestCategory: InterestCategory) {
        realm.writeBlocking {
            copyToRealm(interestCategory, updatePolicy = UpdatePolicy.ALL)
        }
    }

    fun getInterestCategories(): List<InterestCategory> {
        return realm.query<InterestCategory>(InterestCategory::class).find()
    }

    fun getInterestCategoryById(_id: String): InterestCategory? {
        return realm.query<InterestCategory>(InterestCategory::class, "_id == $0", ObjectId(_id)).first().find()
    }

    fun deleteInterestCategory(interestCategory: InterestCategory) {
        realm.writeBlocking {
            findLatest(interestCategory)?.let { delete(it) }
        }
    }

    // ----------------------
    // SearchProfile operations
    // ----------------------

    fun addSearchProfile(searchProfile: DomainSearchProfile) {
        realm.writeBlocking {
            val managedProfile = copyToRealm(
                SearchProfile().apply {
                    name = searchProfile.name
                },
                UpdatePolicy.ALL
            )

            searchProfile.interests.forEach { interest ->
                val managedInterest = query<Interest>(Interest::class, "name == $0", interest.name)
                    .first()
                    .find()

                if (managedInterest != null) {
                    managedProfile.interests.add(managedInterest)
                }
            }
        }
    }


    fun getSearchProfiles(): List<SearchProfile> {
        return realm.query<SearchProfile>(SearchProfile::class).find()
    }

    fun deleteSearchProfileByName(name: String) {
        realm.writeBlocking {
            val results = realm.query<SearchProfile>(SearchProfile::class, "name == $0", name).find().toList()
            for (profile in results) {
                findLatest(profile)?.let { liveProfile ->
                    delete(liveProfile)
                }
            }
        }
    }
}