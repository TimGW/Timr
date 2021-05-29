package com.mittylabs.timr.service

import com.mittylabs.timr.notification.Notifications
import com.mittylabs.timr.notification.NotificationsImpl
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