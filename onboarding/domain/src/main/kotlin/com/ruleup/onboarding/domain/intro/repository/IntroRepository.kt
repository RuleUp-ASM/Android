package com.ruleup.onboarding.domain.intro.repository

import com.ruleup.onboarding.domain.entity.AppVersionGate

/**
 * 앱 버전 게이트 조회(GET /v1/intro). 로그인 이전에 호출하는 공개 엔드포인트.
 * 클라 versionCode 를 헤더로 보내 서버가 강제/권장 업데이트를 판정한다.
 */
interface IntroRepository {
    suspend fun getVersionGate(): AppVersionGate
}
