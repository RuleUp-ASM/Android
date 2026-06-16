package com.ruleup.challenge.data.di

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.api.createChallengeApi
import de.jensklingenberg.ktorfit.Ktorfit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Ktorfit 으로 생성한 challenge API 구현을 그래프에 제공한다.
 * createChallengeApi() 는 ktorfit-ksp 가 생성하는 확장 함수다.
 * (KSP 생성물이 androidMain 에 위치하므로 본 DI 모듈도 androidMain 에 둔다.)
 */
@ContributesTo(AppScope::class)
interface ChallengeNetworkModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideChallengeApi(ktorfit: Ktorfit): ChallengeApi = ktorfit.createChallengeApi()
}
