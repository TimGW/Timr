package com.mittylabs.elaps

import com.mittylabs.elaps.prefs.SharedPrefManager
import com.mittylabs.elaps.prefs.SharedPrefs
import com.mittylabs.elaps.service.Notifications
import com.mittylabs.elaps.service.NotificationsImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun providesSharedPreferences(
        sharedPrefManager: SharedPrefManager
    ): SharedPrefs = SharedPrefs(sharedPrefManager)
}