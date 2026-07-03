package com.ruleup.onboarding.domain.intro.usecase

import com.ruleup.onboarding.domain.entity.AppVersionGate
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import javax.inject.Inject

/**
 * 스플래시 진입 시 앱 버전 게이트를 조회한다(GET /v1/intro).
 *
 * 버전 점검 API 장애가 앱 실행 자체를 막지 않도록 **페일오픈**한다 — 네트워크/서버 오류 시 null 을
 * 반환해 호출자(스플래시)가 정상 흐름(자동 로그인)을 그대로 이어가게 한다.
 */
class CheckAppVersionUseCase
    @Inject
    constructor(
        private val introRepository: IntroRepository,
    ) {
        suspend operator fun invoke(): AppVersionGate? = runCatching { introRepository.getVersionGate() }.getOrNull()
    }
