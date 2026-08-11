package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.repository.ExploreRepository
import javax.inject.Inject

/**
 * 챌린지 둘러보기 목록 조회(명세: GET /challenges/explore).
 * 필터(AND)·정렬(6종)·커서 페이지네이션은 서버가 수행하고, 화면은 결과를 이어 붙인다.
 */
class ExploreChallengesUseCase
    @Inject
    constructor(
        private val exploreRepository: ExploreRepository,
    ) {
        suspend operator fun invoke(
            filter: ExploreFilter = ExploreFilter.none,
            sort: ExploreSort = ExploreSort.default,
            cursor: String? = null,
            size: Int? = null,
        ): ExploreResult = exploreRepository.explore(filter, sort, cursor, size)
    }
