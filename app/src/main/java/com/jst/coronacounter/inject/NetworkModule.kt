package com.jst.coronacounter.inject

import com.jst.coronacounter.BuildConfig
import com.jst.coronacounter.network.NetworkExceptionHandler
import com.jst.coronacounter.network.api.ArcgisApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val networkModule = module {
    factory { provideOkHttpClient() }
    factory { provideCoronaVirusApi(get()) }
    single { provideRetrofit(get()) }
    single { NetworkExceptionHandler() }
}

fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
    Retrofit
        .Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

fun provideOkHttpClient(): OkHttpClient =
    OkHttpClient()
        .newBuilder()
        .addInterceptor(
            HttpLogsBeautifier(HttpLoggingInterceptor.Level.BODY, 2, true)
        )
        .build()

fun provideCoronaVirusApi(retrofit: Retrofit): ArcgisApi = retrofit.create(ArcgisApi::class.java)