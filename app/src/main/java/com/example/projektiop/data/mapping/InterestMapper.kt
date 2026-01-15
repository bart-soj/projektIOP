package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.InterestDto
import com.example.projektiop.data.db.realm.objects.Interest
import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.domain.models.InterestCategory as DomainInterestCategory
import com.example.projektiop.data.util.mongoTimestampToRealmInstant
import com.example.projektiop.data.util.realmInstantToMongoTimestamp
import com.example.projektiop.domain.models.Interest as DomainInterest

fun InterestDto.toRealm(dbRepository: RealmDBRepository): Interest {
    require(!this._id.isNullOrBlank()) {"Missing interest id"}
    val id = this._id
    require(!this.name.isNullOrBlank()) {"Missing interest name"}
    val name = this.name
    require(!this.category.isNullOrBlank()) {"Missing interest category"}
    val categoryId = this.category
    val interestCategory = dbRepository.getInterestCategoryById(categoryId)
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

fun InterestDto.toDomain(dbRepository: RealmDBRepository): DomainInterest {
    return this.toRealm(dbRepository).toDomain()
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

fun Interest.toDomain(): DomainInterest {
    assert(this.category != null) // a interest in db must have a category
    return DomainInterest(
        id = this._id.toHexString(),
        name = this.name,
        category = DomainInterestCategory(id = this.category!!._id.toHexString(), name = this.category!!.name ),
        description = this.description
    )
}