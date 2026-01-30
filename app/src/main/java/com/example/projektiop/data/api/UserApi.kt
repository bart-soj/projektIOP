package com.example.projektiop.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.DELETE

interface UserApi {
    @GET("users/profile")
    suspend fun getMyProfile(): Response<UserProfileResponse>

    @PUT("users/profile")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): Response<UserProfileResponse>

    @Multipart
    @PUT("users/profile/avatar")
    suspend fun uploadAvatar(@Part avatarImage: MultipartBody.Part): Response<UploadAvatarResponse>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<UserProfileResponse>

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<UserSearchDto>>

    @POST("users/profile/interests")
    suspend fun addUserInterest(@Body body: AddUserInterestRequest): Response<UserInterestDto>

    @PUT("users/profile/interests/{userInterestId}")
    suspend fun updateUserInterest(
        @Path("userInterestId") userInterestId: String,
        @Body body: UpdateUserInterestRequest
    ): Response<UserInterestDto>

    @DELETE("users/profile/interests/{userInterestId}")
    suspend fun removeUserInterest(@Path("userInterestId") userInterestId: String): Response<UserProfileResponse>

    @DELETE("users/profile")
    suspend fun deleteOwnAccount(): Response<Unit>
}

data class UserProfileResponse(
    val profile: ProfileDto? = ProfileDto(),
    val _id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val role: String? = null,
    val isBanned: Boolean? = null,
    val banReason: String? = null,
    val bannedAt: String? = null,
    val isTestAccount: Boolean? = null,
    val isEmailVerified: Boolean? = null,
    val isDeleted: Boolean? = null,
    val deletedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val __v: Int? = null,
    val interests: List<UserInterestDto>? = emptyList()
) {
    val effectiveDisplayName: String?
        get() = profile?.displayName
    val effectiveDescription: String?
        get() = profile?.bio
}


data class UploadAvatarResponse(
    val message: String?,
    val avatarUrl: String?,
    val user: UserProfileResponse
)


data class UserInterestDto(
    val userInterestId: String?,
    val interest: InterestDto,
    val customDescription: String? = null
)

data class InterestDto(
    val _id: String? = null,
    val name: String? = null,
    val category: String? = null,
    val description: String? = null,
    val isArchived: Boolean? = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ProfileDto(
    val displayName: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val location: String? = null,
    val birthDate: String? = null,
    val broadcastMessage: String? = null,
    val avatarUrl: String? = null
)

data class UserSearchDto(
    val _id: String? = null,
    val username: String? = null,
    val profile: ProfileDto? = null
)

data class UserStats(
    val friends: Int? = null,
    val posts: Int? = null,
    val likes: Int? = null
)

data class UpdateProfileRequest(
    val profile: ProfileDto
)

data class AddUserInterestRequest(
    val interestId: String,
    val customDescription: String? = null
)

data class UpdateUserInterestRequest(
    val customDescription: String? = null
)