package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import javax.inject.Inject

/**
 * 실시간 인기 챌린지 조회(명세: GET /challenges/trending).
 *
 * 순위는 서버가 24시간 신규 참여 수로 산정하며 클라이언트는 그대로 신뢰한다. 서버가 Top 20 을 주고
 * 홈은 상위 일부만 쓴다. 응답의 `calculatedAt` 은 **최대 10분 지연**된 스냅샷 기준 시각이다.
 */
class GetTrendingChallengesUseCase
    @Inject
    constructor(
        private val exploreRepository: ExploreRepository,
    ) {
        suspend operator fun invoke(category: Category? = null): TrendingSnapshot = exploreRepository.getTrending(category)
    }
