package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 추천 루틴 탭으로 초안을 만든다 (명세 POST /challenges/recommendation/by-template — 경로 A).
 *
 * LLM 을 거치지 않으므로 대기가 없고 rate limit·폴백도 없다. 이후 확인 화면·생성 흐름은 경로 B 와 같다.
 */
class CreateDraftFromTemplateUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(templateId: Long): DraftResult.Ok = challengeRepository.createDraftFromTemplate(templateId)
    }
