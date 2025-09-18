package com.example.projektiop.data.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.api.UpdateProfileRequest
import com.example.projektiop.data.api.Profile
import com.example.projektiop.data.api.AddUserInterestRequest
import com.example.projektiop.data.api.UserInterestDto
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.mapping.toUserProfileResponse
import com.example.projektiop.data.repositories.DBRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID


private const val PREFS_NAME = "auth_prefs"
private const val EMAIL = "my_email"
private const val ID = "_id"


object UserRepository {
    private var id: String? = null
    private var email: String? = null
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            email = prefs?.getString(EMAIL, null) ?: ""
        }
    }

    suspend fun fetchMyProfile(): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.userApi.getMyProfile()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Save ID in SharedPreferences
                val tmpId: String? = body._id
                if (!tmpId.isNullOrBlank()) {
                    SharedPreferencesRepository.set(ID, tmpId.toString())
                }

                // Save user in DB
                try {
                    DBRepository.addLocalUser(body.toRealm())
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving to database: $e")
                    )
                }

                return@withContext Result.success(body)
            } else {
                val tmpId: String = SharedPreferencesRepository.get(ID, "")
                // API failed → fallback to DB
                val localUser = DBRepository.getLocalUserById(tmpId)
                if (localUser != null && !tmpId.isNullOrBlank()) {
                    return@withContext Result.success(localUser.toUserProfileResponse())
                }
                return@withContext Result.failure(Exception("API failed and no local data available"))
            }
        } catch (e: Exception) {
            // Network call threw exception → fallback to DB
            val tmpId: String = SharedPreferencesRepository.get(ID, "")
            // API failed → fallback to DB
            val localUser = DBRepository.getLocalUserById(tmpId)
            if (localUser != null && !tmpId.isNullOrBlank()) {
                return@withContext Result.success(localUser.toUserProfileResponse())
            }
            return@withContext Result.failure(Exception("API error and no local data available $e"))
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
                profile = Profile(
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

                // Save ID in SharedPreferences
                val tmpId: String? = body._id
                if (!tmpId.isNullOrBlank()) {
                    SharedPreferencesRepository.set(ID, tmpId.toString())
                }

                // Save user in DB
                try {
                    DBRepository.addLocalUser(body.toRealm())
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving to database: $e")
                    )
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

                // Save user in DB
                try {
                    DBRepository.addLocalUser(body.toRealm())
                } catch (e: Exception) {
                    return@withContext Result.failure<UserProfileResponse>(
                        Exception("Error saving to database: $e")
                    )
                }

                Result.success(response.body()!!)
            } else {
                // API failed → fallback to DB
                val localUser = DBRepository.getLocalUserById(id)
                if (localUser != null && !id.isNullOrBlank()) {
                    return@withContext Result.success(localUser.toUserProfileResponse())
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
            val localUser = DBRepository.getLocalUserById(id)
            if (localUser != null && !id.isNullOrBlank()) {
                return@withContext Result.success(localUser.toUserProfileResponse())
            }
            return@withContext Result.failure(Exception("API error and no local data available $e"))
        }
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
                    DBRepository.addLocalUser(body.toRealm())
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

    // Fetch public interests catalog; returns map name->id for quick lookup
    suspend fun fetchInterestsMap(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp = com.example.projektiop.data.api.RetrofitInstance.publicInterestApi.getPublicInterests()
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!
                Result.success(list.associate { it.name to it._id })
            } else {
                val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                Result.failure(Exception("Nie udało się pobrać listy zainteresowań${if (!err.isNullOrBlank()) ": ${err.take(200)}" else ""}"))
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
            val nameToId = fetchInterestsMap().getOrElse { return@withContext Result.failure(it) }

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
                val id = jsonIdToString(ui.userInterestId) ?: continue
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
                try { DBRepository.addLocalUser(body.toRealm()) } catch (_: Exception) {}
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
