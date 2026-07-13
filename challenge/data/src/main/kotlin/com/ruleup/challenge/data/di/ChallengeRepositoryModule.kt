package com.ruleup.challenge.data.di

import com.ruleup.challenge.data.notification.SetupNotifierImpl
import com.ruleup.challenge.data.repository.ChallengeRepositoryImpl
import com.ruleup.challenge.data.repository.ExploreRepositoryImpl
import com.ruleup.challenge.data.repository.MyChallengeStoreImpl
import com.ruleup.challenge.data.repository.TargetAppStoreImpl
import com.ruleup.challenge.data.repository.WatcherRepositoryImpl
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.challenge.domain.repository.SetupNotifier
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.challenge.domain.repository.WatcherRepository
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
    abstract fun bindExploreRepository(impl: ExploreRepositoryImpl): ExploreRepository

    @Binds
    @Singleton
    abstract fun bindWatcherRepository(impl: WatcherRepositoryImpl): WatcherRepository

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
