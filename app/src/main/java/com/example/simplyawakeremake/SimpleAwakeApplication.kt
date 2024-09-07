package com.example.simplyawakeremake

import android.app.Application
import com.example.simplyawakeremake.di.appModule
import com.example.simplyawakeremake.di.dataModule
import com.example.simplyawakeremake.di.repositoryModule
import com.example.simplyawakeremake.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext.startKoin

class SimpleAwakeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SimpleAwakeApplication)
            modules(appModule, dataModule, repositoryModule, uiModule)
        }

    }
}