package com.ruleup.android_ruleup.di

import com.ruleup.android_ruleup.notification.SetupNotifierImpl
import com.ruleup.challenge.domain.SetupNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 생성 후 셋업 유도 로컬 알림 바인딩(app 계층). */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    @Singleton
    abstract fun bindSetupNotifier(impl: SetupNotifierImpl): SetupNotifier
}
