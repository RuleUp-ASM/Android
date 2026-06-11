package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.ChallengeRepository
import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import dev.zacsweers.metro.Inject

/**
 * 챌린지 LLM 기본값 추천 유스케이스 (명세 3.1).
 *
 * 제목/설명을 받아 구속력 없는 추천 초안을 돌려준다. 상태 저장 없음.
 * 실패는 예외([com.ruleup.network.dto.ApiException] 등)로 전파된다.
 */
class RecommendChallengeUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(
            title: String,
            description: String? = null,
        ): ChallengeRecommendation =
            challengeRepository.recommend(
                title = title,
                description = description,
            )
    }
