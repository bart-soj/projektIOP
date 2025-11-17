package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.db.objects.UserInterest
import com.example.projektiop.data.repositories.DBRepository

fun UserInterestDto.toRealm(userId: String): UserInterest {

    require(!this.userInterestId.isNullOrBlank()) { "Missing user interest id" }
    val id = this.userInterestId
    // require(!this.userId.isNullOrBlank()) { "Missing user id" } // TODO: add it serverside
    // val userId = this.userId

    require(!this.interest._id.isNullOrBlank()) { "Missing interest id" }
    val interestId = this.interest._id

    return UserInterest.create(
        id = id,
        userId = userId,
        interestId = interestId,
        customDescription = this.customDescription ?: "",
        // createdAt = TODO(),
        // updatedAt = TODO()
    )
}

fun UserInterest.toDto(): UserInterestDto {
    return UserInterestDto(
        userInterestId = this.id,
        // userId = TODO() serverside
        interest = DBRepository.getLocalInterestById(this.interestId)!!.toDto(),
        customDescription = this.customDescription
    )
}