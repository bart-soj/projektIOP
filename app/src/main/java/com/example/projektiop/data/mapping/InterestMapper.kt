package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.InterestDto
import com.example.projektiop.data.db.objects.Interest

fun InterestDto.toRealm(): Interest {
    require(!this._id.isNullOrBlank()) {"Missing interest id"}
    val id = this._id
    require(!this.name.isNullOrBlank()) {"Missing interest name"}
    val name = this.name
    require(!this.category.isNullOrBlank()) {"Missing interest category"}
    val categoryId = this.category

    val createdAt = mongoTimestampToRealmInstant(this.createdAt)
    val updatedAt = mongoTimestampToRealmInstant(this.updatedAt)

    return Interest.create(
        id = id,
        name = name,
        categoryId = categoryId,
        description = this.description ?: "",
        isArchived = this.isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Interest.toDto(): InterestDto {
    return InterestDto(
        _id = this.id,
        name = this.name,
        category = this.categoryId,
        description = this.description,
        isArchived = this.isArchived,
        createdAt = realmInstantToMongoTimestamp(this.createdAt),
        updatedAt = realmInstantToMongoTimestamp(this.updatedAt)
    )
}