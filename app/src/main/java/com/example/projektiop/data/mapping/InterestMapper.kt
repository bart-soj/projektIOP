package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.InterestDto
import com.example.projektiop.data.db.objects.Interest
import com.example.projektiop.data.repositories.DBRepository

fun InterestDto.toRealm(): Interest {
    require(!this._id.isNullOrBlank()) {"Missing interest id"}
    val id = this._id
    require(!this.name.isNullOrBlank()) {"Missing interest name"}
    val name = this.name
    require(!this.category.isNullOrBlank()) {"Missing interest category"}
    val categoryId = this.category
    val interestCategory = DBRepository.getInterestCategoryById(categoryId)
    require(interestCategory != null) {"No such InterestCategory"}

    val createdAt = mongoTimestampToRealmInstant(this.createdAt)
    val updatedAt = mongoTimestampToRealmInstant(this.updatedAt)

    return Interest.create(
        id = id,
        name = name,
        categoryId = categoryId,
        interestCategory = interestCategory,
        description = this.description ?: "",
        isArchived = this.isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Interest.toDto(): InterestDto {
    return InterestDto(
        _id = this._id.toString(),
        name = this.name,
        category = this.categoryId.toString(),
        description = this.description,
        isArchived = this.isArchived,
        createdAt = realmInstantToMongoTimestamp(this.createdAt),
        updatedAt = realmInstantToMongoTimestamp(this.updatedAt)
    )
}