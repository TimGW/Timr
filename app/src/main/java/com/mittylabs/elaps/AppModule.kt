package com.mittylabs.elaps

import com.mittylabs.elaps.prefs.SharedPrefManager
import com.mittylabs.elaps.prefs.SharedPrefs
import com.mittylabs.elaps.service.Notifications
import com.mittylabs.elaps.service.NotificationsImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { SharedPrefManager(androidContext()) }
    single { SharedPrefs(get()) }
    factory<Notifications> { NotificationsImpl(androidContext()) }
}