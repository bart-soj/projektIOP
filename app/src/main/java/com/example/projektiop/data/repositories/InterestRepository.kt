package com.example.projektiop.data.repositories

import android.util.Log
import com.example.projektiop.data.api.InterestDto
import com.example.projektiop.data.api.PublicInterestCategoryDto
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.db.objects.Interest
import com.example.projektiop.data.db.objects.InterestCategory
import com.example.projektiop.data.mapping.toDto
import com.example.projektiop.data.mapping.toRealm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

object InterestRepository {

    suspend fun init() {
        getPublicInterests() // TODO() they dont end up in db
    }

    suspend fun getPublicInterestCategories(): Result<List<PublicInterestCategoryDto>> = withContext(
        Dispatchers.IO) {
        try {
            val response = RetrofitInstance.publicInterestApi.getCategories()
            if (response.isSuccessful && response.body().orEmpty() != emptyList<PublicInterestCategoryDto>()){
                val body = response.body()!!

                for (category in body) {
                    try {
                        val realmCategory = category.toRealm()
                        DBRepository.addInterestCategory(realmCategory)
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Error saving user to database: $e"))
                    }
                }

                return@withContext Result.success(body)

            } else {
                val local = DBRepository.getInterestCategories()
                if (local.orEmpty() != emptyList<InterestCategory>()){
                    return@withContext Result.success(local.map{it.toDto()})
                } else {
                    return@withContext Result.failure(Exception("API failed and no local data"))
                }
            }
        } catch (e: Exception) {
            val local = DBRepository.getInterestCategories()
            if (local != emptyList<InterestCategory>()){
                return@withContext Result.success(local.map{it.toDto()})
            } else {
                return@withContext Result.failure(Exception("API error and no local data $e"))
            }
        }
    }

    suspend fun getPublicInterests(): Result<List<InterestDto>> = withContext(
        Dispatchers.IO) {
        try {
            val response = RetrofitInstance.publicInterestApi.getPublicInterests()
            if (response.isSuccessful && response.body().orEmpty() != emptyList<InterestDto>()){
                val body = response.body()!!
                var returnList = emptyList<InterestDto>()

                for (publicInterestDto in body) {
                    val interestDto = InterestDto(
                        _id = publicInterestDto._id,
                        name = publicInterestDto.name,
                        category = publicInterestDto.category?._id,
                        description = publicInterestDto.description,
                        isArchived = publicInterestDto.isArchived,
                        createdAt = publicInterestDto.createdAt,
                        updatedAt = publicInterestDto.updatedAt
                    )
                    val publicInterestCategoryDto = publicInterestDto.category
                    try {
                        val realmInterest = interestDto.toRealm()
                        val realmCategory = publicInterestCategoryDto?.toRealm()
                        if (realmCategory != null) {
                            DBRepository.addInterestCategory(realmCategory)
                        }
                        DBRepository.addInterest(realmInterest)
                        returnList += interestDto
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Error saving public interest to database: $e"))
                    }
                }

                return@withContext Result.success(returnList)

            } else {
                val local = DBRepository.getInterests()
                if (local.orEmpty() != emptyList<Interest>()){
                    return@withContext Result.success(local!!.map{it.toDto()})
                } else {
                    return@withContext Result.failure(Exception("API failed and no local data"))
                }
            }
        } catch (e: Exception) {
            val local = DBRepository.getInterests()
            if (local.orEmpty() != emptyList<Interest>()){
                return@withContext Result.success(local!!.map{it.toDto()})
            } else {
                return@withContext Result.failure(Exception("API error and no local data $e"))
            }
        }
    }


    // Fetch public interests catalog; returns map name->id for quick lookup
    suspend fun fetchPublicInterestsMap(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        val resp = getPublicInterests().fold(
            onSuccess = { list ->
                Result.success(list.associate {
                    assert(it.name != null)
                    assert(it._id != null)
                    it.name!! to it._id!!
                })
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
        return@withContext resp
    }

    suspend fun resolveIncomingUserInterests(userInterests: List<UserInterestDto>, userId: String, flowToUpdate: MutableStateFlow<List<UserInterestDto>?>) {
        var localInterestCategories: List<String> = DBRepository.getInterestCategories()
            .map{ it._id.toHexString() }

        if (localInterestCategories == emptyList<String>()) {
            getPublicInterestCategories().onSuccess {
                localInterestCategories = it.mapNotNull{ it._id } // just need a list of ids
            }.onFailure { e -> throw e }
        }


        for (userInterestDto in userInterests) {
            runCatching {
                val interestRealm = userInterestDto.interest.toRealm()
                val userInterestRealm = userInterestDto.toRealm(userId)
                val interestCategory = userInterestDto.interest.category
                if (interestCategory !in localInterestCategories ) {
                    getPublicInterests().onSuccess {
                        localInterestCategories = DBRepository.getInterestCategories()
                            .map{ it._id.toHexString() }
                    }
                    if (interestCategory !in localInterestCategories) {
                        throw Exception("invalid interest category id $interestCategory")
                    }
                }
                DBRepository.addInterestPair(interestRealm, userInterestRealm)
            }.onFailure { e ->
                Log.d("INT", "UserInterest resolution failed", e)
            }
        }

        flowToUpdate.value = DBRepository.getUserInterestsByUserId(userId).map{it.toDto()}
    }
}



