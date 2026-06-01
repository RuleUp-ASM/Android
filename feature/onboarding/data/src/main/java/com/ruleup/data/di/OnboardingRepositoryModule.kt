package com.ruleup.data.di

import com.ruleup.data.repository.AuthRepositoryImpl
import com.ruleup.data.repository.ProfileRepositoryImpl
import com.ruleup.data.repository.SessionRepositoryImpl
import com.ruleup.domain.repository.AuthRepository
import com.ruleup.domain.repository.ProfileRepository
import com.ruleup.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
