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
 * Ktorfit 으로 생성한 onboarding API 구현을 그래프에 제공한다(iOS 타깃 공유 구현).
 * createAuthApi()/createProfileApi() 는 ktorfit-ksp 가 타깃별로 생성하는 확장 함수이며,
 * 이 파일은 각 iOS leaf 소스셋(iosX64Main/iosArm64Main/iosSimulatorArm64Main)에 srcDir 로 등록돼
 * 해당 타깃의 생성물과 함께 컴파일된다. (androidMain 의 OnboardingNetworkModule 과 동일 내용)
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
