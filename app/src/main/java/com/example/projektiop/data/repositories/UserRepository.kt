package com.example.projektiop.data.repositories

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.api.UpdateProfileRequest
import com.example.projektiop.data.api.ProfileDto
import com.example.projektiop.data.api.AddUserInterestRequest
import com.example.projektiop.data.api.UpdateUserInterestRequest
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UserApi
import com.example.projektiop.data.api.UserSearchDto
import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.data.mapping.toDomain
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.mapping.toUserProfileResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.UUID
import com.example.projektiop.domain.models.Interest as DomainInterest
import com.example.projektiop.domain.models.UserInterest as DomainUserInterest
import com.example.projektiop.domain.models.User as DomainUser


private const val ID = "_id"


class UserRepository(private val userApi: UserApi,
                     private val dbRepository: RealmDBRepository,
                     private val sharedDataSource: SharedDataSource,
                     private val interestRepository: InterestRepository) {
    private lateinit var id: String
    private val _MyUserInterests = MutableStateFlow<List<DomainUserInterest>?>(null)
    val MyUserInterests: StateFlow<List<DomainUserInterest>?> = _MyUserInterests.asStateFlow()

    private val _repositoryCache = MutableStateFlow<Map<String, OtherUserRepository>>(emptyMap())
    val repositoryCache: StateFlow<Map<String, OtherUserRepository>> =
        _repositoryCache.asStateFlow()

    private val _myUser = MutableStateFlow<DomainUser?>(null)
    val myUser = _myUser.asStateFlow()

    init {
        updateMyId()
    }

    fun updateMyId() {
        val tmpId = sharedDataSource.get(ID, "")
        if (tmpId.isNotBlank()) {
            id = tmpId
        }
    }

    suspend fun ensureRepository(id: String): Result<OtherUserRepository> =
        withContext(Dispatchers.IO) {
            if (id in repositoryCache.value) {
                return@withContext Result.success(repositoryCache.value[id]!!)
            } else {
                fetchUserById(id).onSuccess {
                    val tmpRep = OtherUserRepository(
                        id,
                        userApi,
                        dbRepository,
                        interestRepository
                    )
                    _repositoryCache.value += Pair(id, tmpRep)
                    return@withContext Result.success(repositoryCache.value[id]!!)
                }.onFailure { res -> return@withContext Result.failure(res) }
                return@withContext Result.failure(Exception("Can't ensure repository"))
            }
        }


    suspend fun getMyProfile(): Result<DomainUser> {
        val local = dbRepository.getUserById(id)
        if (local != null) {
            _myUser.value = local.toDomain()
            return Result.success(local.toDomain())
        }

        try {
            val response = userApi.getMyProfile()
            if(!response.isSuccessful) throw HttpException(response)

            val body = response.body()!!
            val userInterests: List<UserInterestDto> =
                body.interests.orEmpty().filter { it.interest != null }

            val tmpId: String? = body._id

            dbRepository.addUser(body.toRealm())

            interestRepository.resolveIncomingUserInterests(userInterests, tmpId!!, _MyUserInterests)

            val local = dbRepository.getUserById(id)!!
            _myUser.value = local.toDomain()

            return Result.success(local.toDomain())

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }


    suspend fun fetchMyProfile(): Result<UserProfileResponse> = withContext(Dispatchers.IO) {


        try {
            val response = userApi.getMyProfile()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val userInterests: List<UserInterestDto> =
                    body.interests.orEmpty().filter { it.interest != null }

                val tmpId: String? = body._id

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
                    interestRepository.resolveIncomingUserInterests(
                        userInterests,
                        tmpId!!,
                        _MyUserInterests
                    )
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving interests to database: $e")
                    )
                }

                // Save ID in SharedPreferences
                if (tmpId.isNotBlank()) {
                    sharedDataSource.set(ID, tmpId.toString())
                    updateMyId()
                }

                _myUser.value = dbRepository.getUserById(id)!!.toDomain()
                return@withContext Result.success(body)
            } else {
                val tmpId: String = sharedDataSource.get(ID, "")
                // API failed → fallback to DB
                if (tmpId.isBlank()) {
                    return@withContext Result.failure(Exception("API failed and no user ID found in preferences"))
                }
                val localUser = dbRepository.getUserById(tmpId)
                if (localUser != null) {
                    _myUser.value = localUser.toDomain()
                    val userProfile = localUser.toUserProfileResponse()
                    return@withContext Result.success(userProfile)
                }
                return@withContext Result.failure(Exception("API failed and no local data available"))
            }
        } catch (e: Exception) {
            // Network call threw exception → fallback to DB
            val tmpId: String = sharedDataSource.get(ID, "")
            if (tmpId.isBlank()) {
                return@withContext Result.failure(Exception("API error and no user ID found in preferences: $e"))
            }
            val localUser = dbRepository.getUserById(tmpId)
            if (localUser != null) {
                _myUser.value = localUser.toDomain()
                val userProfile = localUser.toUserProfileResponse()
                return@withContext Result.success(userProfile)
            }
            return@withContext Result.failure(Exception("API error and no local data available: $e"))
        }
    }


    suspend fun updateMyProfile(
        displayName: String?,
        gender: String?,
        location: String?,
        bio: String?,
        birthDate: String?,
        broadcastMessage: String?
    ): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val body = UpdateProfileRequest(
                profile = ProfileDto(
                    displayName = displayName?.takeIf { it.isNotBlank() },
                    gender = gender?.takeIf { it.isNotBlank() },
                    location = location?.takeIf { it.isNotBlank() },
                    bio = bio?.takeIf { it.isNotBlank() },
                    birthDate = birthDate?.takeIf { it.isNotBlank() },
                    broadcastMessage = broadcastMessage?.takeIf { it.isNotBlank() }
                )
            )
            val response = userApi.updateMyProfile(body)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val userInterests: List<UserInterestDto> =
                    body.interests.orEmpty().filterNotNull().filter { it.interest != null }
                val tmpId: String? = body._id

                // Save user in DB
                try {
                    dbRepository.addUser(body.toRealm())
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving to database: $e")
                    )
                }

                // Save interests in DB
                try {
                    interestRepository.resolveIncomingUserInterests(
                        userInterests,
                        tmpId!!,
                        _MyUserInterests
                    )
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving interests to database: $e")
                    )
                }

                _myUser.value = dbRepository.getUserById(id)!!.toDomain()
                Result.success(response.body()!!)
            } else {
                val errBody = try {
                    response.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                val msg = buildString {
                    append("Nie udało się zaktualizować profilu (${response.code()})")
                    if (!errBody.isNullOrBlank()) append(": ").append(errBody.take(300))
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun fetchUserById(id: String): Result<UserProfileResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = userApi.getUserById(id)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val userInterests: List<UserInterestDto> =
                        body.interests.orEmpty().filterNotNull().filter { it.interest != null }

                    // Save user in DB
                    try {
                        dbRepository.addUser(body.toRealm())
                    } catch (e: Exception) {
                        return@withContext Result.failure<UserProfileResponse>(
                            Exception("Error saving to database: $e")
                        )
                    }

                    // Save interests in DB
                    try {
                        interestRepository.resolveIncomingUserInterests(
                            userInterests,
                            body._id!!,
                            MutableStateFlow(null)
                        ) // TODO() placeholder mutable stateflow for now
                    } catch (e: Exception) {
                        return@withContext Result.failure<UserProfileResponse>(
                            Exception("Error saving interests to database: $e")
                        )
                    }

                    Result.success(response.body()!!)
                } else {
                    // API failed → fallback to DB
                    val localUser = dbRepository.getUserById(id)
                    if (localUser != null) {
                        val userProfile = localUser.toUserProfileResponse()
                        return@withContext Result.success(userProfile)
                    }

                    val errBody = try {
                        response.errorBody()?.string()
                    } catch (_: Exception) {
                        null
                    }
                    val msg = buildString {
                        append("Nie udało się pobrać użytkownika po id z api, brak lokalnych danych (${response.code()})")
                        if (!errBody.isNullOrBlank()) append(": ").append(errBody.take(300))
                    }
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                // API failed → fallback to DB
                val localUser = dbRepository.getUserById(id)
                if (localUser != null) {
                    val userProfile = localUser.toUserProfileResponse()
                    return@withContext Result.success(userProfile)
                }
                return@withContext Result.failure(Exception("API error and no local data available: $e"))
            }
        }


    suspend fun searchUsers(query: String): Result<List<UserSearchDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = userApi.searchUsers(query)
                if (response.isSuccessful) {
                    Result.success(response.body().orEmpty())
                } else {
                    Result.failure(Exception("Błąd wyszukiwania (${response.code()})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


    suspend fun uploadAvatar(
        bytes: ByteArray,
        originalFileName: String? = null,
        mimeType: String? = null
    ): Result<String> {
        return try {
            val safeMime = (mimeType ?: "image/jpeg").toMediaTypeOrNull()
            val requestBody: RequestBody = bytes.toRequestBody(safeMime)
            val fileName =
                originalFileName?.takeIf { it.isNotBlank() } ?: "avatar_${UUID.randomUUID()}.jpg"
            val part = MultipartBody.Part.createFormData(
                name = "avatarImage",
                filename = fileName,
                body = requestBody
            )

            val response = userApi.uploadAvatar(part)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val newUrl = body.avatarUrl

                if(newUrl == null) throw IllegalArgumentException("no avatar url")

                try {
                    dbRepository.updateUserAvatar(id, newUrl)
                } catch (e: Exception) {
                    Log.w(
                        "AVATAR",
                        "uploadAvatar: failed to save avatar to db, will continue. ${e.message}"
                    )
                }

                Result.success(newUrl)
            } else {
                val errBody = try {
                    response.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                val msg = buildString {
                    append("Nie udało się wgrać avatara (${response.code()})")
                    if (!errBody.isNullOrBlank()) append(": ").append(errBody.take(300))
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // TODO make it respond to individual failures not just all failing
    suspend fun syncMyInterestsWithDescriptions(
        desired: Map<DomainInterest, String>
    ): Result<Unit> {

        val current = MyUserInterests.value
            ?.associateBy(
                keySelector = { it.interest.id },
                { Pair(it.customDescription, it.id) }
            ) ?: emptyMap()

        val desiredById = desired.entries.associate {
            it.key.id to it.value.takeIf(String::isNotBlank)
        }

        val toAdd = desiredById.keys - current.keys
        val toRemove = current.keys - desiredById.keys
        val toUpdate = desiredById.keys intersect current.keys

        var success = true

        toAdd.forEach { id ->
            if (!userApi.addUserInterest(
                    AddUserInterestRequest(id, desiredById[id])
                ).isSuccessful
            ) success = false
        }

        toRemove.forEach { id ->
            if (!userApi.removeUserInterest(id).isSuccessful)
                success = false
        }

        toUpdate.forEach { id ->
            if (current[id]?.first != desiredById[id]) {
                if (!userApi.updateUserInterest(
                        current[id]?.second ?: return@forEach,
                        UpdateUserInterestRequest(desiredById[id])
                    ).isSuccessful
                ) success = false
            }
        }

        return if (success) Result.success(Unit)
        else Result.failure(Exception("Failed to sync interests"))
    }


    suspend fun deleteMyAccount(): Result<Unit> {
        try {
            val result = userApi.deleteOwnAccount()
            if(result.isSuccessful){
                return Result.success(Unit)
            } else {
                throw HttpException(result)
            }
        } catch(e: Exception) {
            return Result.failure(e)
        }
    }
}


