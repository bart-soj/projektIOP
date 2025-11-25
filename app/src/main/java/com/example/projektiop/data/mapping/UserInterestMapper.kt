package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.db.objects.UserInterest
import com.example.projektiop.data.repositories.DBRepository

fun UserInterestDto.toRealm(userId: String): UserInterest {

    require(!this.userInterestId.isNullOrBlank()) { "Missing user interest id" }
    val id = this.userInterestId
    require(userId.isNotBlank()) { "Missing user id" }

    require(!this.interest._id.isNullOrBlank()) { "Missing interest id" }
    val interestId = this.interest._id
    val interest = DBRepository.getInterestById(interestId)
    require(interest != null) { "No such Interest" }

    return UserInterest.create(
        id = id,
        userId = userId,
        interestId = interestId,
        interest = interest,
        customDescription = this.customDescription ?: "",
        // createdAt = TODO(),
        // updatedAt = TODO()
    )
}

fun UserInterest.toDto(): UserInterestDto {
    return UserInterestDto(
        userInterestId = this._id.toString(),
        interest = this.interest!!.toDto(),
        customDescription = this.customDescription
    )
}