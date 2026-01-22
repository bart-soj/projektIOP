package com.example.projektiop.data.repositories

import com.example.projektiop.data.api.UserApi
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.data.mapping.toDomain
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.mapping.toUserProfileResponse
import com.example.projektiop.data.util.checkDataFreshness
import com.example.projektiop.domain.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.collections.filterNotNull
import kotlin.collections.orEmpty
import java.time.Duration
import com.example.projektiop.domain.models.UserInterest as DomainUserInterest

private val userFreshnessTimeout: Duration = Duration.ofMinutes(5)

class OtherUserRepository(val id: String,
                          private val userApi: UserApi,
                          private val dbRepository: RealmDBRepository,
                          private val interestRepository: InterestRepository) {

    companion object {
        private val _repositoryCache =
            MutableStateFlow<Map<String, OtherUserRepository>>(emptyMap())
        val repositoryCache: StateFlow<Map<String, OtherUserRepository>> =
            _repositoryCache.asStateFlow()

        fun ensureRepository (
            id: String, userApi: UserApi, dbRepository: RealmDBRepository,
            interestRepository: InterestRepository
        ): OtherUserRepository {
            if (id !in repositoryCache.value) {
                val newRepository =
                    OtherUserRepository(id, userApi, dbRepository, interestRepository)
                _repositoryCache.value += Pair(id, newRepository)
            }
            val repository = repositoryCache.value[id]!!
            return repository
        }
    }

    lateinit var chatId: String
    lateinit var friendshipId: String

    private val _Profile = MutableStateFlow<User?>(null)
    val Profile: StateFlow<User?> = _Profile.asStateFlow()
    private val _UserInterests = MutableStateFlow<List<DomainUserInterest>?>(null)
    val UserInterests: StateFlow<List<DomainUserInterest>?> = _UserInterests.asStateFlow()


    suspend fun fetchProfile(): Result<User> {
        val localUser = dbRepository.getUserById(id)
        val updatedAt = localUser?.updatedAt
        if (localUser != null && updatedAt != null) {
            if (checkDataFreshness(updatedAt, userFreshnessTimeout)) {
                return Result.success(localUser.toDomain())
            }
        }
        return fetchProfileFromApi()
    }

    suspend fun fetchProfileFromApi(): Result<User> {
        try {
            val response = userApi.getUserById(id)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val userInterests: List<UserInterestDto> =
                    body.interests.orEmpty().filterNotNull().filter{ it.interest != null }

                val tmpId: String? = id

                // Save user in DB, mapper ensures mandatory fields present
                try {
                    dbRepository.addUser(body.toRealm())
                } catch (e: Exception) {
                    return Result.failure<User>(
                        Exception("Error saving user to database: $e")
                    )
                }

                // Save interests in DB
                try {
                    interestRepository.resolveIncomingUserInterests(userInterests, tmpId!!, _UserInterests)
                } catch (e: Exception) {
                    return Result.failure<User>(
                        Exception("Error saving interests to database: $e")
                    )
                }

                val domainUser = body.toRealm().toDomain()
                _Profile.value = domainUser
                return Result.success(domainUser)
            } else {

                val localUser = dbRepository.getUserById(id)
                if (localUser != null) {
                    val userProfile = localUser.toDomain()
                    _Profile.value = userProfile
                    return Result.success(userProfile)
                }
                return Result.failure(Exception("API failed and no local data available"))
            }
        } catch (e: Exception) {
            val localUser = dbRepository.getUserById(id)
            if (localUser != null) {
                val userProfile = localUser.toDomain()
                _Profile.value = userProfile
                return Result.success(userProfile)
            }
            return Result.failure(Exception("API error and no local data available: $e"))
        }
    }
}