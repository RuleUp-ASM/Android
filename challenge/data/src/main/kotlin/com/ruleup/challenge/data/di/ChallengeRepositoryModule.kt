package com.ruleup.challenge.data.di

import com.ruleup.challenge.data.repository.ChallengeRepositoryImpl
import com.ruleup.challenge.domain.ChallengeRepository
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
}
