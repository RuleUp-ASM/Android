package com.ruleup.onboarding.data.di

import com.ruleup.onboarding.data.api.AuthApi
import com.ruleup.onboarding.data.api.ProfileApi
import com.ruleup.onboarding.data.api.createAuthApi
import com.ruleup.onboarding.data.api.createProfileApi
import de.jensklingenberg.ktorfit.Ktorfit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Ktorfit 으로 생성한 onboarding API 구현을 그래프에 제공한다.
 * createAuthApi()/createProfileApi() 는 ktorfit-ksp 가 생성하는 확장 함수다.
 */
@ContributesTo(AppScope::class)
interface OnboardingNetworkModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideAuthApi(ktorfit: Ktorfit): AuthApi = ktorfit.createAuthApi()

    @Provides
    @SingleIn(AppScope::class)
    fun provideProfileApi(ktorfit: Ktorfit): ProfileApi = ktorfit.createProfileApi()
}
