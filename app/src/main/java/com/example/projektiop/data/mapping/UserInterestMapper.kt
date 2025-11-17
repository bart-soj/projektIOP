package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.db.objects.UserInterest

fun UserInterestDto.toRealm(userId: String): UserInterest {

    val id = this.userInterestId?: throw IllegalArgumentException("Missing user interest id")
    // val userId = this.userId?: throw IllegalArgumentException("Missing user id") TODO() add it serverside
    val interestId = this.interest._id ?: throw IllegalArgumentException("Missing interest id")

    return UserInterest.create(
        id = id,
        userId = userId,
        interestId = interestId,
        customDescription = this.customDescription ?: "",
        // createdAt = TODO(),
        // updatedAt = TODO()
    )
}