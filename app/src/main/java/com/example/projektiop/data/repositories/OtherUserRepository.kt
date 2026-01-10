package com.example.projektiop.data.repositories

import com.example.projektiop.data.api.UserApi
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.mapping.toUserProfileResponse
import com.example.projektiop.util.checkDataFreshness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.collections.filterNotNull
import kotlin.collections.orEmpty
import java.time.Duration
import com.example.projektiop.domain.models.UserInterest as DomainUserInterest

private val userFreshnessTimeout: Duration = Duration.ofMinutes(5)

class OtherUserRepository(private val id: String,
                          private val userApi: UserApi,
                          private val dbRepository: RealmDBRepository,
                          private val interestRepository: InterestRepository) {
    lateinit var chatId: String
    lateinit var friendshipId: String

    private val _Profile = MutableStateFlow<UserProfileResponse?>(null)
    val Profile: StateFlow<UserProfileResponse?> = _Profile.asStateFlow()
    private val _UserInterests = MutableStateFlow<List<DomainUserInterest>?>(null)
    val UserInterests: StateFlow<List<DomainUserInterest>?> = _UserInterests.asStateFlow()

    /*
    val userFlow: Flow<User?>
        get() {
            return dbRepository.getUserFlowById(this.id)
        }
     */

    suspend fun init() {
        fetchProfile()
    }

    suspend fun fetchProfile(): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        val localUser = dbRepository.getUserById(id)
        val updatedAt = localUser?.updatedAt
        if (localUser != null && updatedAt != null) {
            if (checkDataFreshness(updatedAt, userFreshnessTimeout)) {
                return@withContext Result.success(localUser.toUserProfileResponse())
            } else {
                fetchProfileFromApi()
            }
        } else {
            fetchProfileFromApi()
        }
    }

    suspend fun fetchProfileFromApi(): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
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
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving user to database: $e")
                    )
                }

                // Save interests in DB
                try {
                    interestRepository.resolveIncomingUserInterests(userInterests, tmpId!!, _UserInterests)
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving interests to database: $e")
                    )
                }

                _Profile.value = body
                return@withContext Result.success(body)
            } else {

                val localUser = dbRepository.getUserById(id)
                if (localUser != null) {
                    val userProfile = localUser.toUserProfileResponse()
                    _Profile.value = userProfile
                    return@withContext Result.success(userProfile)
                }
                return@withContext Result.failure(Exception("API failed and no local data available"))
            }
        } catch (e: Exception) {
            val localUser = dbRepository.getUserById(id)
            if (localUser != null) {
                val userProfile = localUser.toUserProfileResponse()
                _Profile.value = userProfile
                return@withContext Result.success(userProfile)
            }
            return@withContext Result.failure(Exception("API error and no local data available: $e"))
        }
    }

    fun getId(): String {
        return id
    }
}