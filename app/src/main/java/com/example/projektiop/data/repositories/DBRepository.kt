package com.example.projektiop.data.repositories

import com.example.projektiop.data.db.objects.Friendship
import com.example.projektiop.data.db.objects.Message
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.db.objects.UserInterest
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy


object DBRepository {
    private lateinit var realm: Realm

    fun init(realmInstance: Realm) {
        realm = realmInstance
    }

    // ----------------------
    // Message operations
    // ----------------------
    fun getLocalMessages(): List<Message> {
        return realm.query<Message>(Message::class).find()
    }

    fun getLocalMessagesByChat(chatId: String): List<Message> {
        return realm.query<Message>(Message::class, "chatId == $0", chatId).find()
    }

    fun addLocalMessage(message: Message) {
        realm.writeBlocking {
            copyToRealm(message)
        }
    }

    fun updateLocalMessage(message: Message) {
        realm.writeBlocking {
            findLatest(message)?.let {
                it.content = message.content
                it.readBy = message.readBy
                it.updatedAt = message.updatedAt
            }
        }
    }

    fun deleteLocalMessage(message: Message) {
        realm.writeBlocking {
            findLatest(message)?.let { delete(it) }
        }
    }

    // ----------------------
    // User operations
    // ----------------------
    fun getLocalUsers(): List<User> {
        return realm.query<User>(User::class).find()
    }

    fun getLocalUserById(userId: String): User? {
        return realm.query<User>(User::class, "id == $0", userId).first().find()
    }

    fun getLocalUserByEmail(email: String): User? {
        return realm.query<User>(User::class, "email == $0", email).first().find()
    }

    fun addLocalUser(user: User) {
        realm.writeBlocking {
            // UpdatePolicy.ALL means the object will be updated if already exists
            copyToRealm(user, updatePolicy = UpdatePolicy.ALL)
        }
    }

    fun updateLocalUser(user: User) {
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

    fun deleteLocalUser(user: User) {
        realm.writeBlocking {
            findLatest(user)?.let { delete(it) }
        }
    }

    // ----------------------
    // Friendship operations
    // ----------------------
    fun getLocalFriendships(): List<Friendship> {
        return realm.query<Friendship>(Friendship::class).find()
    }

    fun addLocalFriendship(friendship: Friendship) {
        realm.writeBlocking {
            copyToRealm(friendship) // will throw an exception if pk already exists
        }
    }

    fun updateLocalFriendship(friendship: Friendship) {
        realm.writeBlocking {
            findLatest(friendship)?.let {
                it.status = friendship.status
                it.friendshipType = friendship.friendshipType
                it.blockedBy = friendship.blockedBy
                it.isBlocked = friendship.isBlocked
                it.updatedAt = friendship.updatedAt
            }
        }
    }

    fun deleteLocalFriendship(friendship: Friendship) {
        realm.writeBlocking {
            findLatest(friendship)?.let { delete(it) }
        }
    }

    // ----------------------
    // UserInterest operations
    // ----------------------
    fun getLocalUserInterests(): List<UserInterest> {
        return realm.query<UserInterest>(UserInterest::class).find()
    }

    fun addLocalUserInterest(userInterest: UserInterest) {
        realm.writeBlocking {
            copyToRealm(userInterest)
        }
    }

    fun updateLocalUserInterest(userInterest: UserInterest) {
        realm.writeBlocking {
            findLatest(userInterest)?.let {
                it.customDescription = userInterest.customDescription
                it.updatedAt = userInterest.updatedAt
            }
        }
    }

    fun deleteLocalUserInterest(userInterest: UserInterest) {
        realm.writeBlocking {
            findLatest(userInterest)?.let { delete(it) }
        }
    }
}