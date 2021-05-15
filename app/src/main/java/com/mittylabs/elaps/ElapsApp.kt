package com.mittylabs.elaps

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.mittylabs.elaps.prefs.SharedPrefs
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module

class ElapsApp : Application() {
    private val sharedPref: SharedPrefs by inject()
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

        val nightMode = when (sharedPref.getDarkModeSetting()) {
            0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
