package com.mittylabs.timr.app

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun providesSharedPreferences(
        sharedPrefManager: SharedPrefManager
    ): SharedPrefs = SharedPrefs(sharedPrefManager)
}