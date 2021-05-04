package com.mittylabs.elaps

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module

class ElapsApp : Application() {
    private val appComponent: List<Module> = listOf(
        appModule,
        viewModelModule
    )

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.DEBUG)
            androidContext(this@ElapsApp)
            modules(appComponent)
        }
    }
}
