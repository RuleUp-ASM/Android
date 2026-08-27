package com.ruleup.onboarding.data.di

import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.intro.api.IntroApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnboardingNetworkModule {
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()

    @Provides
    @Singleton
    fun provideIntroApi(retrofit: Retrofit): IntroApi = retrofit.create()
}
