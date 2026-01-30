package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.PublicInterestCategoryDto
import com.example.projektiop.data.db.realm.objects.InterestCategory
import io.realm.kotlin.types.RealmInstant
import com.example.projektiop.domain.models.InterestCategory as DomainInterestCategory

fun PublicInterestCategoryDto.toRealm(): InterestCategory {
    require(!this._id.isNullOrBlank()) { "Missing interest category id" }
    val id = this._id
    require(!this.name.isNullOrBlank()) { "Missing interest category name" }
    val name = this.name



    return InterestCategory.create(
        id = id,
        name = name,
        updatedAt = RealmInstant.now()
    )
}

fun InterestCategory.toDto(): PublicInterestCategoryDto {
    return PublicInterestCategoryDto(
        _id = this._id.toString(),
        name = this.name
    )
}


fun InterestCategory.toDomain(): DomainInterestCategory {
    return DomainInterestCategory(
        id = this._id.toHexString(),
        name = this.name
    )
}


fun PublicInterestCategoryDto.toDomain(): DomainInterestCategory {
    return this.toRealm().toDomain()
}