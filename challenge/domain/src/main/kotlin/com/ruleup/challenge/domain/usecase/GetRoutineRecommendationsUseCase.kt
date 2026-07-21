package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.RoutineRecommendation
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 루틴 발견 추천 조회 (명세: GET /recommendations/routines).
 * 탐색 상단 "추천 루틴" 섹션 렌더링에 사용한다. 실패해도 섹션만 숨기면 되므로 호출부에서 흡수한다.
 */
class GetRoutineRecommendationsUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(limit: Int? = null): List<RoutineRecommendation> = challengeRepository.recommendRoutines(limit)
    }
