package com.example.simplyawakeremake

import android.app.Application
import com.example.simplyawakeremake.di.appModule
import com.example.simplyawakeremake.di.dataModule
import com.example.simplyawakeremake.di.repositoryModule
import org.koin.core.context.GlobalContext.startKoin

class SimpleAwakeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            modules(appModule, dataModule, repositoryModule)
        }

    }
}