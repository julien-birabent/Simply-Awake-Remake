package com.simplyawakeremake.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.simplyawakeremake.BuildConfig
import com.simplyawakeremake.data.track.TrackService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val dataModule = module {

    single<Gson> {
        GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .setLenient()
            .create()
    }

    single<OkHttpClient> {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        OkHttpClient
            .Builder()
            .addInterceptor(interceptor)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()

    }

    single<Retrofit> {
        val rxAdapter = RxJava3CallAdapterFactory.create()
        Retrofit.Builder()
            .client(get())
            .baseUrl(BuildConfig.baseServerUrl)
            .addCallAdapterFactory(rxAdapter)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> Retrofit.getService(serviceClass : Class<T>) : T = create(serviceClass)

    fun provideTrackService(retrofit: Retrofit): TrackService =
        retrofit.getService(TrackService::class.java)


    single<TrackService> { provideTrackService(get()) }

}