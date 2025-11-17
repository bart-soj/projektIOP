package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.InterestDto
import com.example.projektiop.data.db.objects.Interest

fun InterestDto.toRealm(): Interest {

    val id = this._id ?: throw IllegalArgumentException("Missing interest id")
    val name = this.name ?: throw IllegalArgumentException("Missing interest name")
    val categoryId = this.category ?: throw IllegalArgumentException("Missing category id")

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