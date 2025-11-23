package com.example.projektiop.data.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.api.UpdateProfileRequest
import com.example.projektiop.data.api.ProfileDto
import com.example.projektiop.data.api.AddUserInterestRequest
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.api.UpdateUserInterestRequest
import com.example.projektiop.data.api.UserSearchDto
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.mapping.toUserProfileResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID


private const val PREFS_NAME = "auth_prefs"
private const val EMAIL = "my_email"
private const val ID = "_id"


object UserRepository {
    private lateinit var id: String
    // private var email: String? = null
    private var prefs: SharedPreferences? = null
    private val _MyUserInterests = MutableStateFlow<List<UserInterestDto>?>(null)
    val MyUserInterests: StateFlow<List<UserInterestDto>?> = _MyUserInterests.asStateFlow()

    suspend fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            updateMyId()
        }
    }

    fun updateMyId() {
        val tmpId = SharedPreferencesRepository.get(ID, "")
        if (tmpId.isNotBlank()) {
            id = tmpId
        }
    }

    fun getMyUserFlow(): Flow<User?> {
        return DBRepository.getUserFlowById(id)
    }

    suspend fun fetchMyProfile(): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.userApi.getMyProfile()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val userInterests: List<UserInterestDto> =
                    body.interests.orEmpty().filterNotNull().filter{ it.interest != null }

                val tmpId: String? = body._id

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
                    InterestRepository.resolveIncomingUserInterests(userInterests, tmpId!!, _MyUserInterests)
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving interests to database: $e")
                    )
                }

                // Save ID in SharedPreferences
                if (!tmpId.isNullOrBlank()) {
                    SharedPreferencesRepository.set(ID, tmpId.toString())
                }

                return@withContext Result.success(body)
            } else {
                val tmpId: String = SharedPreferencesRepository.get(ID, "")
                // API failed → fallback to DB
                if (tmpId.isBlank()) {
                    return@withContext Result.failure(Exception("API failed and no user ID found in preferences"))
                }
                val localUser = DBRepository.getUserById(tmpId)
                if (localUser != null) {
                    val userProfile = localUser.toUserProfileResponse()
                    return@withContext Result.success(userProfile)
                }
                return@withContext Result.failure(Exception("API failed and no local data available"))
            }
        } catch (e: Exception) {
            // Network call threw exception → fallback to DB
            val tmpId: String = SharedPreferencesRepository.get(ID, "")
            if (tmpId.isBlank()) {
                return@withContext Result.failure(Exception("API error and no user ID found in preferences: $e"))
            }
            val localUser = DBRepository.getUserById(tmpId)
            if (localUser != null) {
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
            val response = RetrofitInstance.userApi.updateMyProfile(body)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val userInterests: List<UserInterestDto> =
                    body.interests.orEmpty().filterNotNull().filter{ it.interest != null }
                val tmpId: String? = body._id

                // Save user in DB
                try {
                    DBRepository.addUser(body.toRealm())
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving to database: $e")
                    )
                }

                // Save interests in DB
                try {
                    InterestRepository.resolveIncomingUserInterests(userInterests, tmpId!!, _MyUserInterests)
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving interests to database: $e")
                    )
                }

                // Save ID in SharedPreferences
                if (!tmpId.isNullOrBlank()) {
                    SharedPreferencesRepository.set(ID, tmpId.toString())
                }

                Result.success(response.body()!!)
            } else {
                val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
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


    suspend fun fetchUserById(id: String): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.userApi.getUserById(id)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val userInterests: List<UserInterestDto> =
                    body.interests.orEmpty().filterNotNull().filter{ it.interest != null }

                // Save user in DB
                try {
                    DBRepository.addUser(body.toRealm())
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving to database: $e")
                    )
                }

                // Save interests in DB
                try {
                    InterestRepository.resolveIncomingUserInterests(userInterests, body._id!!, MutableStateFlow(null)) // TODO() placeholder mutable stateflow for now
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving interests to database: $e")
                    )
                }

                Result.success(response.body()!!)
            } else {
                // API failed → fallback to DB
                val localUser = DBRepository.getUserById(id)
                if (localUser != null) {
                    val userProfile = localUser.toUserProfileResponse()
                    return@withContext Result.success(userProfile)
                }

                val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                val msg = buildString {
                    append("Nie udało się pobrać użytkownika po id z api, brak lokalnych danych (${response.code()})")
                    if (!errBody.isNullOrBlank()) append(": ").append(errBody.take(300))
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            // API failed → fallback to DB
            val localUser = DBRepository.getUserById(id)
            if (localUser != null) {
                val userProfile = localUser.toUserProfileResponse()
                return@withContext Result.success(userProfile)
            }
            return@withContext Result.failure(Exception("API error and no local data available: $e"))
        }
    }


    suspend fun searchUsers(query: String): Result<List<UserSearchDto>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.userApi.searchUsers(query)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("Błąd wyszukiwania (${response.code()})"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }


    suspend fun uploadAvatar(
        bytes: ByteArray,
        originalFileName: String? = null,
        mimeType: String? = null
    ): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val safeMime = (mimeType ?: "image/jpeg").toMediaTypeOrNull()
            val requestBody: RequestBody = bytes.toRequestBody(safeMime)
            val fileName = originalFileName?.takeIf { it.isNotBlank() } ?: "avatar_${UUID.randomUUID()}.jpg"
            val part = MultipartBody.Part.createFormData(
                name = "avatarImage",
                filename = fileName,
                body = requestBody
            )

            val response = RetrofitInstance.userApi.uploadAvatar(part)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val tmpId: String? = try { body._id } catch (_: Throwable) { null }
                if (!tmpId.isNullOrBlank()) {
                    SharedPreferencesRepository.set(ID, tmpId.toString())
                }
                // Try to persist locally, but don't fail the whole operation if local save has issues
                try {
                    DBRepository.addUser(body.toRealm())
                } catch (e: Exception) {
                    Log.w("UserRepository", "uploadAvatar: failed to persist locally, will continue. ${e.message}")
                }
                // Best effort: refresh full profile (backend may return partial)
                try {
                    val refreshed = fetchMyProfile().getOrNull()
                    if (refreshed != null) return@withContext Result.success(refreshed)
                } catch (_: Exception) { /* ignore */ }
                Result.success(body)
            } else {
                val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
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




    // Sync interests: desiredNames is set of interest names selected in UI.
    // We diff against server's current interests and call add/remove endpoints.
    suspend fun syncMyInterestsByNames(desiredNames: Set<String>): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            // 1) Get current profile to know existing interests
            val current = fetchMyProfile().getOrElse { return@withContext Result.failure(it) }
            val currentInterests: List<UserInterestDto> = current.interests ?: emptyList()
            val currentNames = currentInterests.mapNotNull { it.interest.name }.toSet()

            // 2) Build diffs
            val toAdd = desiredNames.minus(currentNames)
            val toRemoveNames = currentNames.minus(desiredNames)
            val toRemove = currentInterests.filter { it.interest.name in toRemoveNames }

            // 3) Map names -> ids via public catalog
            val nameToId = InterestRepository.fetchPublicInterestsMap().getOrElse { return@withContext Result.failure(it) }

            // 4) Execute adds
            for (name in toAdd) {
                val id = nameToId[name] ?: continue
                val resp = com.example.projektiop.data.api.RetrofitInstance.userApi.addUserInterest(
                    AddUserInterestRequest(interestId = id, customDescription = null)
                )
                if (!resp.isSuccessful) {
                    val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                    return@withContext Result.failure(Exception("Dodanie zainteresowania nie powiodło się (${resp.code()})${if(!err.isNullOrBlank()) ": ${err.take(200)}" else ""}"))
                }
            }

            // 5) Execute removals
            fun jsonIdToString(el: com.google.gson.JsonElement?): String? = try {
                when {
                    el == null || el.isJsonNull -> null
                    el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString
                    el.isJsonObject && el.asJsonObject.has("_id") -> el.asJsonObject.get("_id").asString
                    else -> el.toString()
                }
            } catch (_: Exception) { null }

            for (ui in toRemove) {
                val id = ui.userInterestId ?: continue
                val resp = com.example.projektiop.data.api.RetrofitInstance.userApi.removeUserInterest(id)
                if (!resp.isSuccessful) {
                    val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                    return@withContext Result.failure(Exception("Usunięcie zainteresowania nie powiodło się (${resp.code()})${if(!err.isNullOrBlank()) ": ${err.take(200)}" else ""}"))
                }
            }

            // 6) Refresh and persist
            val refreshed = RetrofitInstance.userApi.getMyProfile()
            if (refreshed.isSuccessful && refreshed.body() != null) {
                val body = refreshed.body()!!
                try { DBRepository.addUser(body.toRealm()) } catch (_: Exception) {}
                Result.success(body)
            } else {
                val err = try { refreshed.errorBody()?.string() } catch (_: Exception) { null }
                Result.failure(Exception("Nie udało się odświeżyć profilu po zmianie zainteresowań${if(!err.isNullOrBlank()) ": ${err.take(200)}" else ""}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
// TODO: zerknąć tutaj
    suspend fun syncMyInterestsWithDescriptions(desired: Map<String, String?>): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            // 1) Current profile with interests
            val current = fetchMyProfile().getOrElse { return@withContext Result.failure(it) }
            val currentInterests: List<UserInterestDto> = current.interests ?: emptyList()
            val currentNames = currentInterests.mapNotNull { it.interest.name }.toSet()

            val desiredNames = desired.keys

            // 2) Diff
            val toAdd = desiredNames.minus(currentNames)
            val toRemoveNames = currentNames.minus(desiredNames.toSet())
            val toKeepNames = desiredNames.intersect(currentNames)

            fun jsonIdToString(el: com.google.gson.JsonElement?): String? = try {
                when {
                    el == null || el.isJsonNull -> null
                    el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString
                    el.isJsonObject && el.asJsonObject.has("_id") -> el.asJsonObject.get("_id").asString
                    else -> el.toString()
                }
            } catch (_: Exception) { null }

            // 3) Map names -> ids
            val nameToId = InterestRepository.fetchPublicInterestsMap().getOrElse { return@withContext Result.failure(it) }

            // 4) Adds (pass description if provided)
            for (name in toAdd) {
                val id = nameToId[name] ?: continue
                val customDesc = desired[name]?.takeIf { !it.isNullOrBlank() }
                val resp = RetrofitInstance.userApi.addUserInterest(
                    AddUserInterestRequest(interestId = id, customDescription = customDesc)
                )
                if (!resp.isSuccessful) {
                    val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                    return@withContext Result.failure(Exception("Dodanie zainteresowania nie powiodło się (${resp.code()})${if(!err.isNullOrBlank()) ": ${err.take(200)}" else ""}"))
                }
            }

            // 5) Removes
            val toRemove = currentInterests.filter { it.interest.name in toRemoveNames }
            for (ui in toRemove) {
                val id = ui.userInterestId ?: continue
                val resp = RetrofitInstance.userApi.removeUserInterest(id)
                if (!resp.isSuccessful) {
                    val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                    return@withContext Result.failure(Exception("Usunięcie zainteresowania nie powiodło się (${resp.code()})${if(!err.isNullOrBlank()) ": ${err.take(200)}" else ""}"))
                }
            }

            // 6) Updates of descriptions for kept interests (only when changed)
            val kept = currentInterests.filter { it.interest.name in toKeepNames }
            for (ui in kept) {
                val name = ui.interest.name
                val desiredRaw = desired[name]
                val desiredDesc = desiredRaw?.trim() ?: ""
                val currentDesc = ui.customDescription?.trim().orEmpty()
                if (desiredDesc != currentDesc) {
                    val id = ui.userInterestId ?: continue
                    val toSend = if (desiredDesc.isBlank() && currentDesc.isNotBlank()) "" else desiredDesc.ifBlank { null }
                    val resp = RetrofitInstance.userApi.updateUserInterest(
                        userInterestId = id,
                        body = UpdateUserInterestRequest(customDescription = toSend)
                    )
                    if (!resp.isSuccessful) {
                        val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                        return@withContext Result.failure(Exception("Aktualizacja opisu zainteresowania nie powiodła się (${resp.code()})${if(!err.isNullOrBlank()) ": ${err.take(200)}" else ""}"))
                    }
                }
            }

            // 7) Refresh and persist
            val refreshed = RetrofitInstance.userApi.getMyProfile()
            if (refreshed.isSuccessful && refreshed.body() != null) {
                val body = refreshed.body()!!
                try { DBRepository.addUser(body.toRealm()) } catch (_: Exception) {}
                Result.success(body)
            } else {
                val err = try { refreshed.errorBody()?.string() } catch (_: Exception) { null }
                Result.failure(Exception("Nie udało się odświeżyć profilu po zmianie zainteresowań${if(!err.isNullOrBlank()) ": ${err.take(200)}" else ""}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

