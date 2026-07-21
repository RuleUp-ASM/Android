package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 선택한 루틴 템플릿 기반 챌린지 설정 초안 생성 (명세: POST /challenges/recommendation/by-template).
 * 응답은 LLM 추천([ChallengeRepository.recommend])과 동일 스키마라 확인 화면 폼 채움 로직을 재사용한다.
 */
class RecommendChallengeByTemplateUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(templateId: Long): ChallengeRecommendation = challengeRepository.recommendByTemplate(templateId)
    }
