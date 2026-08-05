package com.ruleup.onboarding.data.di

import com.ruleup.domain.token.TokenRefresher
import com.ruleup.onboarding.data.auth.repository.AuthRepositoryImpl
import com.ruleup.onboarding.data.auth.repository.TokenRefresherImpl
import com.ruleup.onboarding.data.device.DeviceIdentityRepositoryImpl
import com.ruleup.onboarding.data.intro.repository.IntroRepositoryImpl
import com.ruleup.onboarding.data.profile.repository.OnboardingProfileRepositoryImpl
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.auth.repository.DeviceIdentityRepository
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.onboarding.domain.profile.OnboardingProfileRepository
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
    abstract fun bindTokenRefresher(impl: TokenRefresherImpl): TokenRefresher

    @Binds
    @Singleton
    abstract fun bindOnboardingProfileRepository(impl: OnboardingProfileRepositoryImpl): OnboardingProfileRepository

    @Binds
    @Singleton
    abstract fun bindIntroRepository(impl: IntroRepositoryImpl): IntroRepository

    @Binds
    @Singleton
    abstract fun bindDeviceIdentityRepository(impl: DeviceIdentityRepositoryImpl): DeviceIdentityRepository
}
