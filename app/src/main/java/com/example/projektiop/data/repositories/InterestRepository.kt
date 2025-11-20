package com.example.projektiop.data.repositories

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object InterestRepository {

    suspend fun getPublicInterestCategories(): Result<List<PublicInterestCategoryDto>> = withContext(
        Dispatchers.IO) {
        try {
            val response = RetrofitInstance.publicInterestApi.getCategories()
            if (response.isSuccessful && response.body().orEmpty() != emptyList<PublicInterestCategoryDto>()){
                val body = response.body()!!

                for (category in body) {
                    try {
                        val realmCategory = category.toRealm()
                        DBRepository.addLocalInterestCategory(realmCategory)
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Error saving user to database: $e"))
                    }
                }

                return@withContext Result.success(body)

            } else {
                val local = DBRepository.getLocalInterestCategories()
                if (local.orEmpty() != emptyList<InterestCategory>()){
                    return@withContext Result.success(local.map{it.toDto()})
                } else {
                    return@withContext Result.failure(Exception("API failed and no local data"))
                }
            }
        } catch (e: Exception) {
            val local = DBRepository.getLocalInterestCategories()
            if (local.orEmpty() != emptyList<InterestCategory>()){
                return@withContext Result.success(local.map{it.toDto()})
            } else {
                return@withContext Result.failure(Exception("API error and no local data"))
            }
        }
    }

    suspend fun getPublicInterests(): Result<List<InterestDto>> = withContext(
        Dispatchers.IO) {
        try {
            val response = RetrofitInstance.publicInterestApi.getPublicInterests()
            if (response.isSuccessful && response.body().orEmpty() != emptyList<InterestDto>()){
                val body = response.body()!!

                for (interest in body) {
                    try {
                        val realmInterest = interest.toRealm()
                        DBRepository.addLocalInterest(realmInterest)
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Error saving user to database: $e"))
                    }
                }

                return@withContext Result.success(body)

            } else {
                val local = DBRepository.getLocalInterests()
                if (local.orEmpty() != emptyList<Interest>()){
                    return@withContext Result.success(local!!.map{it.toDto()})
                } else {
                    return@withContext Result.failure(Exception("API failed and no local data"))
                }
            }
        } catch (e: Exception) {
            val local = DBRepository.getLocalInterests()
            if (local.orEmpty() != emptyList<Interest>()){
                return@withContext Result.success(local!!.map{it.toDto()})
            } else {
                return@withContext Result.failure(Exception("API error and no local data"))
            }
        }
    }


    // Fetch public interests catalog; returns map name->id for quick lookup
    suspend fun fetchPublicInterestsMap(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        val resp = getPublicInterests().fold(
            onSuccess = { list ->
                Result.success(list.associate { it.name to it._id })
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
        return@withContext resp
    }

    suspend fun resolveIncomingUserInterests(userInterests: List<UserInterestDto>, userId: String, flowToUpdate: MutableStateFlow<List<UserInterestDto>?>) {
        var localInterestCategories: List<String> = DBRepository.getLocalInterestCategories()
            .map{ it.id }


        for (userInterestDto in userInterests) {
            runCatching {
                val interestRealm = userInterestDto.interest.toRealm()
                val userInterestRealm = userInterestDto.toRealm(userId)
                val interestCategory = userInterestDto.interest.category
                if (interestCategory !in localInterestCategories) {
                    getPublicInterestCategories().onSuccess {
                        localInterestCategories = it.map{ it._id } // just need a list of ids
                    }
                    if (interestCategory !in localInterestCategories) {
                        throw Exception("invalid interest category id")
                    }
                }
                DBRepository.addLocalInterestPair(interestRealm, userInterestRealm)
            }.onFailure { e ->
                // Log.d("INT", "UserInterest resolution failed", e)
            }
        }

        flowToUpdate.value = DBRepository.getLocalUserInterestsByUserId(userId).map{it.toDto()}
    }
}



