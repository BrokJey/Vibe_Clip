package com.example.vibeclip_frontend.data

import com.example.vibeclip_frontend.BuildConfig
import com.example.vibeclip_frontend.data.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Используем BASE_URL из BuildConfig, чтобы совпадало с плеером
    private const val BASE_URL = BuildConfig.API_BASE_URL
    
//    private val loggingInterceptor = HttpLoggingInterceptor().apply {
//        level = HttpLoggingInterceptor.Level.HEADERS
//    }
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Длинные таймауты для multipart-загрузки видео (десятки МБ по Wi‑Fi). */
    private val uploadOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(15, TimeUnit.MINUTES)
        .callTimeout(20, TimeUnit.MINUTES)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val uploadRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(uploadOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
    val uploadApiService: ApiService = uploadRetrofit.create(ApiService::class.java)
}


