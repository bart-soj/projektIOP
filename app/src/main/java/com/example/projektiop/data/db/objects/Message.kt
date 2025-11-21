package com.example.projektiop.data.db.objects

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.RealmInstant
import java.util.UUID

class Message : RealmObject {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()

    @Index
    var chatId: String = ""

    @Index
    var senderId: String = ""

    var content: String = ""

    var readBy: RealmList<String> = realmListOf()

    var createdAt: RealmInstant? = null
    var updatedAt: RealmInstant? = null
}
