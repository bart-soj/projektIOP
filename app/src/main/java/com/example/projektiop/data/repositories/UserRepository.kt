package com.example.projektiop.data.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.projektiop.data.api.RetrofitInstance
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.api.UpdateProfileRequest
import com.example.projektiop.data.api.Profile
import com.example.projektiop.data.db.objects.User
import com.example.projektiop.data.mapping.toRealm
import com.example.projektiop.data.mapping.toUserProfileResponse
import com.example.projektiop.data.repositories.DBRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
}
