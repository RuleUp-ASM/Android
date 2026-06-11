package com.ruleup.challenge.data.di

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.api.createChallengeApi
import de.jensklingenberg.ktorfit.Ktorfit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Ktorfit 으로 생성한 challenge API 구현을 그래프에 제공한다(iOS 타깃 공유 구현).
 * createChallengeApi() 는 ktorfit-ksp 가 타깃별로 생성하는 확장 함수이며, 이 파일은 각 iOS leaf 소스셋에
 * srcDir 로 등록돼 해당 타깃 생성물과 함께 컴파일된다. (androidMain 의 ChallengeNetworkModule 과 동일 내용)
 */
@ContributesTo(AppScope::class)
interface ChallengeNetworkModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideChallengeApi(ktorfit: Ktorfit): ChallengeApi = ktorfit.createChallengeApi()
}
