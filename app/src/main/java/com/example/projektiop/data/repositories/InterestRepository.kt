package com.example.projektiop.data.repositories

import android.util.Log
import com.example.projektiop.data.api.PublicInterestApi
import com.example.projektiop.data.api.PublicInterestCategoryDto
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.objects.Interest
import com.example.projektiop.data.db.objects.InterestCategory
import com.example.projektiop.data.db.objects.UserInterest
import com.example.projektiop.data.mapping.toDto
import com.example.projektiop.data.mapping.toRealm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InterestRepository {

    // TODO() a flow of MyUserInterests or something similar to display in UI

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

    suspend fun resolveIncomingUserInterests(userInterests: List<UserInterestDto>, userId: String) {
        Log.d("INT", "resolving interests started!")
        var localInterestCategories: List<String> = DBRepository.getLocalInterestCategories()
            .map{ it.id }


        for (userInterestDto in userInterests) {
            runCatching {
                val interestRealm = userInterestDto.interest.toRealm()
                val userInterestRealm = userInterestDto.toRealm(userId)
                val interestCategory = userInterestDto.interest.category
                if (interestCategory !in localInterestCategories) {
                    getPublicInterestCategories().onSuccess { localInterestCategories = it.map{ it._id} }
                    if (interestCategory !in localInterestCategories) {
                        throw Exception("invalid interest category id")
                    }
                }
                DBRepository.addLocalInterestPair(interestRealm, userInterestRealm)
            }.onFailure { e ->
                Log.d("INT", "this one failed", e)
            }
        }
        Log.d("INT", "resolving interests ended!")
    }
}



