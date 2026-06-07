package com.ruleup.onboarding.data.di

import com.ruleup.onboarding.data.repository.AuthRepositoryImpl
import com.ruleup.onboarding.data.repository.ProfileRepositoryImpl
import com.ruleup.onboarding.domain.auth.usecase.AuthRepository
import com.ruleup.onboarding.domain.profile.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface OnboardingRepositoryModule {
    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
