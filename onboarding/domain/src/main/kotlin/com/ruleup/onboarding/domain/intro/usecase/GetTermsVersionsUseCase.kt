package com.ruleup.onboarding.domain.intro.usecase

import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import javax.inject.Inject

/**
 * 가입 화면이 동의 기록에 실을 현행 약관 버전.
 *
 * 인트로를 아직 못 받았거나 조회가 실패했으면(페일오픈) 빈 값으로 떨어진다 —
 * [TermsVersions.of] 가 폴백 버전을 내주고 서버가 재검증한다.
 */
class GetTermsVersionsUseCase
    @Inject
    constructor(
        private val introRepository: IntroRepository,
    ) {
        operator fun invoke(): TermsVersions = introRepository.lastTermsVersions() ?: TermsVersions(emptyMap())
    }
