package com.example.simplyawakeremake.di

import com.example.simplyawakeremake.data.track.TrackService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

const val baseTrackServerUrl = "https://evolv-audio.sfo3.cdn.digitaloceanspaces.com/simply-awake/"


val dataModule = module {

    single<Gson> {
        GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .setLenient()
            .create()
    }

    single<OkHttpClient> {
        OkHttpClient
            .Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()

    }

    single<Retrofit> {
        val rxAdapter = RxJava3CallAdapterFactory.create()
        Retrofit.Builder()
            .client(get())
            .baseUrl(baseTrackServerUrl)
            .addConverterFactory(GsonConverterFactory.create(get()))
            .addCallAdapterFactory(rxAdapter).build()
    }

    fun provideTrackService(retrofit: Retrofit): TrackService =
        retrofit.create(TrackService::class.java)

    single<TrackService> { provideTrackService(get()) }

}