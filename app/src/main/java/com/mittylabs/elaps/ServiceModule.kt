package com.mittylabs.elaps

import com.mittylabs.elaps.service.Notifications
import com.mittylabs.elaps.service.NotificationsImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

@Module
@InstallIn(ServiceComponent::class)
abstract class ServiceModule {
    @Binds
    abstract fun bindNotificationService(
        analyticsServiceImpl: NotificationsImpl
    ): Notifications
}