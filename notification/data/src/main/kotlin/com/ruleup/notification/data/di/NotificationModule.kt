package com.ruleup.notification.data.di

import com.ruleup.notification.data.api.NotificationApi
import com.ruleup.notification.data.repository.NotificationRepositoryImpl
import com.ruleup.notification.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

/** Retrofit 으로 만든 알림 API 를 Hilt 그래프에 제공한다. */
@Module
@InstallIn(SingletonComponent::class)
object NotificationNetworkModule {
    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi = retrofit.create()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
