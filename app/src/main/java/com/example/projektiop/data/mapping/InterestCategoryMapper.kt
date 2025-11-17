package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.PublicInterestCategoryDto
import com.example.projektiop.data.db.objects.InterestCategory

fun PublicInterestCategoryDto.toRealm(): InterestCategory {
    val id = this._id ?: throw IllegalArgumentException("Missing interest id")
    val name = this.name ?: throw IllegalArgumentException("Missing interest name")

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