package com.example.projektiop.data.repositories

import android.util.Log
import com.example.projektiop.data.api.InterestDto
import com.example.projektiop.data.api.PublicInterestApi
import com.example.projektiop.data.api.PublicInterestCategoryDto
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.data.db.realm.objects.Interest
import com.example.projektiop.data.db.realm.objects.InterestCategory
import com.example.projektiop.data.mapping.toDomain
import com.example.projektiop.data.mapping.toDto
import com.example.projektiop.data.mapping.toRealm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.projektiop.domain.models.Interest as DomainInterest

class InterestRepository(private val publicInterestApi: PublicInterestApi,
                         private val dbRepository: RealmDBRepository) {

    private val _publicInterests = MutableStateFlow<List<DomainInterest>>(emptyList())
    val publicInterests = _publicInterests.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            getPublicInterests()
        }
    }

    suspend fun getPublicInterestCategories(): Result<List<PublicInterestCategoryDto>> = withContext(
        Dispatchers.IO) {
        try {
            val response = publicInterestApi.getCategories()
            if (response.isSuccessful && response.body().orEmpty() != emptyList<PublicInterestCategoryDto>()){
                val body = response.body()!!

                for (category in body) {
                    try {
                        val realmCategory = category.toRealm()
                        dbRepository.addInterestCategory(realmCategory)
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Error saving user to database: $e"))
                    }
                }

                return@withContext Result.success(body)

            } else {
                val local = dbRepository.getInterestCategories()
                if (local != emptyList<InterestCategory>()){
                    return@withContext Result.success(local.map{it.toDto()})
                } else {
                    return@withContext Result.failure(Exception("API failed and no local data"))
                }
            }
        } catch (e: Exception) {
            val local = dbRepository.getInterestCategories()
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
            val response = publicInterestApi.getPublicInterests()
            if (response.isSuccessful && response.body().orEmpty() != emptyList<InterestDto>()){
                val body = response.body()!!
                var returnList = emptyList<InterestDto>()
                var domainList = emptyList<DomainInterest>()

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
                        val realmCategory = publicInterestCategoryDto?.toRealm()
                        if (realmCategory != null) {
                            dbRepository.addInterestCategory(realmCategory)
                        }
                        val realmInterest = interestDto.toRealm(dbRepository)
                        val domainInterest = realmInterest.toDomain()
                        dbRepository.addInterest(realmInterest)
                        returnList += interestDto
                        domainList += domainInterest
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Error saving public interest to database: $e"))
                    }
                }

                _publicInterests.value = domainList
                return@withContext Result.success(returnList)
            } else {
                val local = dbRepository.getInterests()
                if (local.orEmpty() != emptyList<Interest>()){
                    val localDomain = local!!.map{it.toDomain()}
                    _publicInterests.value = localDomain
                    return@withContext Result.success(local!!.map{it.toDto()})
                } else {
                    return@withContext Result.failure(Exception("API failed and no local data"))
                }
            }
        } catch (e: Exception) {
            val local = dbRepository.getInterests()
            if (local.orEmpty() != emptyList<Interest>()){
                val localDomain = local!!.map{it.toDomain()}
                _publicInterests.value = localDomain
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
        var localInterestCategories: List<String> = dbRepository.getInterestCategories()
            .map{ it._id.toHexString() }

        if (localInterestCategories == emptyList<String>()) {
            getPublicInterestCategories().onSuccess {
                localInterestCategories = it.mapNotNull{ it._id } // just need a list of ids
            }.onFailure { e -> throw e }
        }


        for (userInterestDto in userInterests) {
            runCatching {
                // first add category
                val interestCategory = userInterestDto.interest.category
                if (interestCategory !in localInterestCategories ) {
                    getPublicInterests().onSuccess {
                        localInterestCategories = dbRepository.getInterestCategories()
                            .map{ it._id.toHexString() }
                    }
                    if (interestCategory !in localInterestCategories) {
                        throw Exception("invalid interest category id $interestCategory")
                    }
                }
                // second add interest
                val interestRealm = userInterestDto.interest.toRealm(dbRepository)
                dbRepository.addInterest(interestRealm)
                // only then add userInterest
                val userInterestRealm = userInterestDto.toRealm(userId, dbRepository)
                dbRepository.addUserInterest(userInterestRealm)
            }.onFailure { e ->
                Log.d("INT", "UserInterest resolution failed", e)
            }
        }

        flowToUpdate.value = dbRepository.getUserInterestsByUserId(userId).map{it.toDto()}
    }

    suspend fun getInterestByName(name: String): DomainInterest? {
        val result =  dbRepository.getInterestByName(name)
        return result?.toDomain()
    }
}



