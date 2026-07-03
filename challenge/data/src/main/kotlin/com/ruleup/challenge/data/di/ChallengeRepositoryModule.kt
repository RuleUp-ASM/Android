package com.ruleup.challenge.data.di

import com.ruleup.challenge.data.notification.SetupNotifierImpl
import com.ruleup.challenge.data.repository.ChallengeRepositoryImpl
import com.ruleup.challenge.data.repository.MyChallengeStoreImpl
import com.ruleup.challenge.data.repository.TargetAppStoreImpl
import com.ruleup.challenge.domain.ChallengeRepository
import com.ruleup.challenge.domain.MyChallengeStore
import com.ruleup.challenge.domain.SetupNotifier
import com.ruleup.challenge.domain.TargetAppStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChallengeRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChallengeRepository(impl: ChallengeRepositoryImpl): ChallengeRepository

    @Binds
    @Singleton
    abstract fun bindMyChallengeStore(impl: MyChallengeStoreImpl): MyChallengeStore

    @Binds
    @Singleton
    abstract fun bindTargetAppStore(impl: TargetAppStoreImpl): TargetAppStore

    @Binds
    @Singleton
    abstract fun bindSetupNotifier(impl: SetupNotifierImpl): SetupNotifier
}
