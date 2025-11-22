package com.example.projektiop.data.repositories

import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.mapping.toUserProfileResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.collections.filterNotNull
import kotlin.collections.orEmpty

class OtherUserRepository(private val id: String) {
    lateinit var chatId: String
    lateinit var friendshipId: String

    private val _Profile = MutableStateFlow<UserProfileResponse?>(null)
    val Profile: StateFlow<UserProfileResponse?> = _Profile.asStateFlow()
    private val _UserInterests = MutableStateFlow<List<UserInterestDto>?>(null)
    val UserInterests: StateFlow<List<UserInterestDto>?> = _UserInterests.asStateFlow()

    suspend fun init() {
        fetchProfile()
    }

    suspend fun fetchProfile(): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.userApi.getUserById(id)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val userInterests: List<UserInterestDto> =
                    body.interests.orEmpty().filterNotNull().filter{ it.interest != null }

                val tmpId: String? = id

                // Save user in DB, mapper ensures mandatory fields present
                try {
                    DBRepository.addUser(body.toRealm())
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving user to database: $e")
                    )
                }

                // Save interests in DB
                try {
                    InterestRepository.resolveIncomingUserInterests(userInterests, tmpId!!, _UserInterests)
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving interests to database: $e")
                    )
                }

                _Profile.value = body
                return@withContext Result.success(body)
            } else {

                val localUser = DBRepository.getUserById(id)
                if (localUser != null) {
                    val userProfile = localUser.toUserProfileResponse()
                    _Profile.value = userProfile
                    return@withContext Result.success(userProfile)
                }
                return@withContext Result.failure(Exception("API failed and no local data available"))
            }
        } catch (e: Exception) {
            val localUser = DBRepository.getUserById(id)
            if (localUser != null) {
                val userProfile = localUser.toUserProfileResponse()
                _Profile.value = userProfile
                return@withContext Result.success(userProfile)
            }
            return@withContext Result.failure(Exception("API error and no local data available: $e"))
        }
    }
}