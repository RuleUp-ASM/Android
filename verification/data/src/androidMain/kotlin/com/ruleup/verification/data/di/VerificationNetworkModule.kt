package com.ruleup.verification.data.di

import com.ruleup.verification.data.api.VerificationApi
import com.ruleup.verification.data.api.createVerificationApi
import de.jensklingenberg.ktorfit.Ktorfit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Ktorfit 으로 생성한 verification API 구현을 그래프에 제공한다.
 * createVerificationApi() 는 ktorfit-ksp 가 생성하는 확장 함수다.
 * (KSP 생성물이 androidMain 에 위치하므로 본 DI 모듈도 androidMain 에 둔다.)
 */
@ContributesTo(AppScope::class)
interface VerificationNetworkModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideVerificationApi(ktorfit: Ktorfit): VerificationApi = ktorfit.createVerificationApi()
}
