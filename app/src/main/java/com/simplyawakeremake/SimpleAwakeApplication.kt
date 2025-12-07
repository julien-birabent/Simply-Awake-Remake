package com.simplyawakeremake

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.simplyawakeremake.di.appModule
import com.simplyawakeremake.di.dataModule
import com.simplyawakeremake.di.databaseModule
import com.simplyawakeremake.di.domainModule
import com.simplyawakeremake.di.repositoryModule
import com.simplyawakeremake.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class SimpleAwakeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SimpleAwakeApplication)
            modules(appModule, dataModule, databaseModule, repositoryModule, domainModule, uiModule)
        }
        logFirebaseInfo()
    }

    fun logFirebaseInfo() {
        val options = FirebaseApp.getInstance().options
        Log.d("FirebaseInfo", "projectId=${options.projectId}, appId=${options.applicationId}")
    }

}