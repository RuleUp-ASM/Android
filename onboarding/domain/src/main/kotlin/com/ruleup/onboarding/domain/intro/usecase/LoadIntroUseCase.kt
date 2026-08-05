package com.ruleup.onboarding.domain.intro.usecase

import com.ruleup.onboarding.domain.entity.IntroInfo
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import javax.inject.Inject

/**
 * 스플래시 진입 시 인트로 정보를 조회한다(GET /v1/intro).
 *
 * 버전 게이트 API 장애가 앱 실행 자체를 막지 않도록 **페일오픈**한다 — 네트워크·서버 오류면 null 을
 * 돌려줘 호출자(스플래시)가 정상 흐름(자동 로그인)을 이어가게 한다. 약관 버전도 함께 못 받게 되지만,
 * 가입 화면이 [com.ruleup.domain.entity.user.TermsVersions.FALLBACK_VERSION] 으로 진행하고
 * 서버가 재검증한다.
 */
class LoadIntroUseCase
    @Inject
    constructor(
        private val introRepository: IntroRepository,
    ) {
        suspend operator fun invoke(): IntroInfo? = runCatching { introRepository.getIntro() }.getOrNull()
    }
