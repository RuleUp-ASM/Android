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
abstract class OnboardingRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
