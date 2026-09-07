package com.ruleup.onboarding.domain.intro.usecase

import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import javax.inject.Inject

/** [LoadIntroUseCase] 의 판정. */
sealed interface IntroGate {
    /**
     * 더 진행하지 않는다. 업데이트 전에는 어떤 화면도 열지 않는다.
     *
     * @property minAppVersion 안내 문구에 넣을 최소 버전. 없으면 화면이 일반 문구로 떨어진다.
     * @property devTestMsg 개발·점검용 문구. **사용자에게 보여주는 값이 아니다** — 진단으로만 쓴다.
     */
    data class ForceUpdate(
        val minAppVersion: String?,
        val devTestMsg: String?,
    ) : IntroGate

    /** 통과. 다음 단계(자동 로그인)로 간다. */
    data object Pass : IntroGate
}

/**
 * 앱 진입 게이트를 판정한다(GET /v1/intro). 판정까지 여기서 끝내야 진입을 막는 조건이 화면 쪽으로
 * 흩어지지 않는다.
 *
 * 게이트 API 장애가 앱 실행을 막지 않도록 **페일오픈**한다 — 오류면 [IntroGate.Pass] 다. 약관 버전도
 * 함께 못 받지만 가입 화면이 `TermsVersions.FALLBACK_VERSION` 으로 진행하고 서버가 재검증한다.
 */
class LoadIntroUseCase
    @Inject
    constructor(
        private val introRepository: IntroRepository,
    ) {
        suspend operator fun invoke(): IntroGate {
            val gate = runCatching { introRepository.getIntro() }.getOrNull()?.versionGate ?: return IntroGate.Pass
            return if (gate.forceUpdate) {
                IntroGate.ForceUpdate(minAppVersion = gate.minAppVersion, devTestMsg = gate.devTestMsg)
            } else {
                IntroGate.Pass
            }
        }
    }
