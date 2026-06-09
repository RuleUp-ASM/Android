package com.ruleup.onboarding.data.di

import com.ruleup.onboarding.data.api.AuthApi
import com.ruleup.onboarding.data.api.ProfileApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import retrofit2.Retrofit

@ContributesTo(AppScope::class)
interface OnboardingNetworkModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideAuthApiService(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @SingleIn(AppScope::class)
    fun provideProfileApiService(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)
}
