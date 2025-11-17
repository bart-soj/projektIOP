package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.PublicInterestCategoryDto
import com.example.projektiop.data.db.objects.InterestCategory

fun PublicInterestCategoryDto.toRealm(): InterestCategory {
    require(!this._id.isNullOrBlank()) { "Missing interest id" }
    val id = this._id
    require(!this.name.isNullOrBlank()) { "Missing interest name" }
    val name = this.name



    return InterestCategory.create(
        id = id,
        name = name,
        // createdAt = TODO(),
        // updatedAt = TODO()
    )
}

fun InterestCategory.toDto(): PublicInterestCategoryDto {
    return PublicInterestCategoryDto(
        _id = this.id,
        name = this.name
    )
}