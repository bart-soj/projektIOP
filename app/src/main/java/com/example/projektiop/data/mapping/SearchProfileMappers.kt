package com.example.projektiop.data.mapping

import com.example.projektiop.domain.models.SearchProfile
import com.example.projektiop.data.db.realm.objects.SearchProfile as RealmSearchProfile


fun RealmSearchProfile.toDomain(): SearchProfile {
    return SearchProfile(
        name = this.name,
        interests = this.interests.map { it.toDomain() }
    )
}



