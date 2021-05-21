package com.mittylabs.elaps.service

import com.mittylabs.elaps.notification.Notifications
import com.mittylabs.elaps.notification.NotificationsImpl
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