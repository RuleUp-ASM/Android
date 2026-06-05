package com.ruleup.data.di

import com.ruleup.data.repository.AuthRepositoryImpl
import com.ruleup.data.repository.ProfileRepositoryImpl
import com.ruleup.domain.onboarding.AuthRepository
import com.ruleup.domain.profile.ProfileRepository
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
