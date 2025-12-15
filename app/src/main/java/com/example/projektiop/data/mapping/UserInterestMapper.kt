package com.example.projektiop.data.mapping

import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.db.realm.objects.UserInterest
import com.example.projektiop.data.db.realm.RealmDBRepository

fun UserInterestDto.toRealm(userId: String, dbRepository: RealmDBRepository): UserInterest {

    require(!this.userInterestId.isNullOrBlank()) { "Missing user interest id" }
    val id = this.userInterestId
    require(userId.isNotBlank()) { "Missing user id" }

    require(!this.interest._id.isNullOrBlank()) { "Missing interest id" }
    val interestId = this.interest._id
    val interest = dbRepository.getInterestById(interestId)
    require(interest != null) { "No such Interest" }

    return UserInterest.create(
        id = id,
        userId = userId,
        interestId = interestId,
        interest = interest,
        customDescription = this.customDescription ?: "",
        // createdAt = TODO(),
    )
}

fun UserInterest.toDto(): UserInterestDto {
    return UserInterestDto(
        userInterestId = this._id.toHexString(),
        interest = this.interest!!.toDto(),
        customDescription = this.customDescription
    )
}