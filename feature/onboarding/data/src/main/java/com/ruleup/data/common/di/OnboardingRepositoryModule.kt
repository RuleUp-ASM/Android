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
interface OnboardingRepositoryModule {
    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
