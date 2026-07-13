package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.repository.ExploreRepository
import javax.inject.Inject

/**
 * 카테고리별 진행 중 챌린지 수 조회(명세: GET /challenge-categories).
 * 탐색 메인 카테고리 그리드에 "N개 챌린지" 로 노출한다(10분 이내 지연 허용 표시용 수치).
 */
class GetChallengeCategoriesUseCase
    @Inject
    constructor(
        private val exploreRepository: ExploreRepository,
    ) {
        suspend operator fun invoke(): List<ChallengeCategoryCount> = exploreRepository.getCategories()
    }
