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
import com.example.projektiop.domain.models.UserInterest as DomainUserInterest
import com.example.projektiop.domain.models.InterestCategory as DomainInterestCategory

class InterestRepository(private val publicInterestApi: PublicInterestApi,
                         private val dbRepository: RealmDBRepository) {

    private val _publicInterests = MutableStateFlow<List<DomainInterest>>(emptyList())
    val publicInterests = _publicInterests.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            getPublicInterests()
        }
    }

    suspend fun getPublicInterestCategories(): Result<List<DomainInterestCategory>> = withContext(
        Dispatchers.IO) {
        try {
            val response = publicInterestApi.getCategories()
            if (response.isSuccessful){
                val body = response.body()!!
                val outList: MutableList<DomainInterestCategory> = mutableListOf()

                for (category in body) {
                    try {
                        val realmCategory = category.toRealm()
                        val domainCategory = realmCategory.toDomain()
                        dbRepository.addInterestCategory(realmCategory)
                        outList.add(domainCategory)
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Error saving user to database: $e"))
                    }
                }

                return@withContext Result.success(outList)

            } else {
                val local = dbRepository.getInterestCategories()
                if (local != emptyList<InterestCategory>()){
                    return@withContext Result.success(local.map{it.toDomain()})
                } else {
                    return@withContext Result.failure(Exception("API failed and no local data"))
                }
            }
        } catch (e: Exception) {
            val local = dbRepository.getInterestCategories()
            if (local != emptyList<InterestCategory>()){
                return@withContext Result.success(local.map{it.toDomain()})
            } else {
                return@withContext Result.failure(Exception("API error and no local data $e"))
            }
        }
    }

    suspend fun getPublicInterests(): Result<List<DomainInterest>> {
        try {
            val response = publicInterestApi.getPublicInterests()
            if (response.isSuccessful && response.body().orEmpty() != emptyList<InterestDto>()){
                val body = response.body()!!
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
                        domainList += domainInterest
                    } catch (e: Exception) {
                        return Result.failure(Exception("Error saving public interest to database: $e"))
                    }
                }

                _publicInterests.value = domainList
                return Result.success(domainList)
            } else {
                val local = dbRepository.getInterests()
                if (local.orEmpty() != emptyList<Interest>()){
                    val localDomain = local!!.map{it.toDomain()}
                    _publicInterests.value = localDomain
                    return Result.success(local.map{it.toDomain()})
                } else {
                    return Result.failure(Exception("API failed and no local data"))
                }
            }
        } catch (e: Exception) {
            val local = dbRepository.getInterests()
            if (local.orEmpty() != emptyList<Interest>()){
                val localDomain = local!!.map{it.toDomain()}
                _publicInterests.value = localDomain
                return Result.success(local.map{it.toDomain()})
            } else {
                return Result.failure(Exception("API error and no local data $e"))
            }
        }
    }


    // Fetch public interests catalog; returns map name->id for quick lookup
    suspend fun fetchPublicInterestsMap(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        val resp = getPublicInterests().fold(
            onSuccess = { list ->
                Result.success(list.associate {
                    it.name to it.id
                })
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
        return@withContext resp
    }

    suspend fun resolveIncomingUserInterests(userInterests: List<UserInterestDto>, userId: String, flowToUpdate: MutableStateFlow<List<DomainUserInterest>?>) {
        var localInterestCategories: List<String> = dbRepository.getInterestCategories()
            .map{ it._id.toHexString() }

        if (localInterestCategories == emptyList<String>()) {
            getPublicInterestCategories().onSuccess {
                localInterestCategories = it.map { it.id } // just need a list of ids
            }.onFailure { e -> throw e }
        }


        for (userInterestDto in userInterests) {
            runCatching {
                // first add category
                val interestCategory = userInterestDto.interest.category
                if (interestCategory !in localInterestCategories ) {
                    getPublicInterests().onSuccess { list ->
                        localInterestCategories = list.map { it.id }
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

        flowToUpdate.value = dbRepository.getUserInterestsByUserId(userId).map{it.toDomain()}
    }

    suspend fun getInterestByName(name: String): DomainInterest? {
        val result =  dbRepository.getInterestByName(name)
        return result?.toDomain()
    }
}



