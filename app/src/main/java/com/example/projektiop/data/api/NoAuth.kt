package com.example.projektiop.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit

class NoAuthClient(val client: OkHttpClient)
class NoAuthRetrofit(val retrofit: Retrofit)