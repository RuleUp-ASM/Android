package com.ruleup.onboarding.domain.intro.repository

import com.ruleup.onboarding.domain.entity.IntroInfo

/**
 * 앱 진입 정보 조회(GET /v1/intro). 로그인 이전에 호출하는 공개 엔드포인트다.
 * `appVersionCode` 와 `platform` 을 헤더로 보내 서버가 강제 업데이트를 판정한다.
 */
interface IntroRepository {
    suspend fun getIntro(): IntroInfo
}
