package com.mittylabs.elaps

import com.mittylabs.elaps.prefs.SharedPrefManager
import com.mittylabs.elaps.prefs.SharedPrefs
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { SharedPrefManager(androidContext()) }
    single { SharedPrefs(get()) }
}