package com.ruleup.onboarding.domain.intro.usecase

import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import javax.inject.Inject

/** [LoadIntroUseCase] 의 판정. */
sealed interface IntroGate {
    /** 더 진행하지 않는다. */
    data class ForceUpdate(
        val minAppVersion: String?,
        val devTestMsg: String?,
    ) : IntroGate

    /** 통과. */
    data object Pass : IntroGate
}

/** 앱 진입 게이트를 판정한다(GET /v1/intro). */
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
