package com.ruleup.data.common.di

import com.ruleup.data.auth.repository.AuthRepositoryImpl
import com.ruleup.data.auth.repository.SessionRepositoryImpl
import com.ruleup.data.profile.repository.ProfileRepositoryImpl
import com.ruleup.domain.auth.repository.AuthRepository
import com.ruleup.domain.auth.repository.SessionRepository
import com.ruleup.domain.profile.repository.ProfileRepository
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
