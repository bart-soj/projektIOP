package com.example.projektiop.data.mapping

import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.domain.models.Interest
import com.example.projektiop.domain.models.InterestCategory
import com.example.projektiop.domain.models.SearchProfile
import io.realm.kotlin.ext.realmListOf
import com.example.projektiop.data.db.realm.objects.SearchProfile as RealmSearchProfile


fun RealmSearchProfile.toDomain(): SearchProfile {
    return SearchProfile(
        name = this.name,
        interests = this.interests.map { it.toDomain() }
    )
}



