package com.example.projektiop.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository

object RetrofitInstance {
    private const val BASE_URL_KEY: String = "BASE_URL"
    private val BASE_URL = SharedPreferencesRepository.get(BASE_URL_KEY, "") + "/api/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = AuthRepository.getToken()
        val builder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val userApi: UserApi by lazy { retrofit.create(UserApi::class.java) }
    val friendshipApi: FriendshipApi by lazy { retrofit.create(FriendshipApi::class.java) }
    val chatApi: ChatApi by lazy { retrofit.create(ChatApi::class.java) }
    val certificateApi: CertificateApi by lazy { retrofit.create(CertificateApi::class.java) }
    val publicInterestApi: PublicInterestApi by lazy { retrofit.create(PublicInterestApi::class.java) }

    fun getBaseUrl(): String {
        return BASE_URL
    }
}
