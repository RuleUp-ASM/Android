package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.entity.user.InterestCategory
import javax.inject.Inject

/**
 * 실시간 인기 챌린지 조회(명세: GET /challenges/trending). 탐색 메인 상단 랭킹에 노출한다.
 * 스코어는 서버가 산정(최근 24시간 참여 이벤트 지수 감쇠 합)하며 클라이언트는 순서만 신뢰한다.
 */
class GetTrendingChallengesUseCase
    @Inject
    constructor(
        private val exploreRepository: ExploreRepository,
    ) {
        suspend operator fun invoke(category: InterestCategory? = null): List<TrendingChallenge> = exploreRepository.getTrending(category)
    }
