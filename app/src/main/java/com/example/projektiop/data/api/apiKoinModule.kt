package com.example.projektiop.data.api


import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.SharedDataSource
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.converter.gson.GsonConverterFactory

val apiKoinModule = module {
    single<String>(qualifier = named("BaseUrl")) {
        get<SharedDataSource>().get("BASE_URL", "") + "/api/"
    }

    single { AuthInterceptor( get() ) }

    single<TokenProvider> { get<AuthRepository>() }

    single { HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY } }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single<retrofit2.Retrofit> {
        retrofit2.Retrofit.Builder()
            .baseUrl(get<String>(named("BaseUrl")))
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<NoAuthClient> {
        NoAuthClient(
            OkHttpClient.Builder()
                .addInterceptor(get<HttpLoggingInterceptor>())
                .build()
        )
    }

    single<NoAuthRetrofit> {
        NoAuthRetrofit (
            retrofit2.Retrofit.Builder()
                .baseUrl(get<String>(named("BaseUrl")))
                .client(get<NoAuthClient>().client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        )
    }

    single<AuthApi> {
        get<NoAuthRetrofit>().retrofit.create(AuthApi::class.java)
    }

    single<UserApi> {
        get<retrofit2.Retrofit>().create(UserApi::class.java)
    }

    single<FriendshipApi> {
        get<retrofit2.Retrofit>().create(FriendshipApi::class.java)
    }

    single<ChatApi> {
        get<retrofit2.Retrofit>().create(ChatApi::class.java)
    }

    single<CertificateApi> {
        get<retrofit2.Retrofit>().create(CertificateApi::class.java)
    }

    single<PublicInterestApi> {
        get<retrofit2.Retrofit>().create(PublicInterestApi::class.java)
    }

    single<BackupApi> {
        get<retrofit2.Retrofit>().create(BackupApi::class.java)
    }

    single<PublicKeyApi> {
        get<retrofit2.Retrofit>().create(PublicKeyApi::class.java)
    }

    single { ErrorConverter(get()) }

    single<ReportApi> { get<retrofit2.Retrofit>().create(ReportApi::class.java) }
}