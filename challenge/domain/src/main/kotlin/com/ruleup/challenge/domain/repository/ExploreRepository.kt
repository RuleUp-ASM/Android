package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.domain.entity.category.Category

/** 챌린지 탐색. */
interface ExploreRepository {
    /** 실시간 인기. */
    suspend fun getTrending(category: Category? = null): TrendingSnapshot

    /** 카테고리 그리드 12종 + 진행 중 공개 그룹 방 수. */
    suspend fun getCategories(): List<ChallengeCategoryCount>

    /** 둘러보기. */
    suspend fun explore(
        filter: ExploreFilter = ExploreFilter.none,
        sort: ExploreSort = ExploreSort.default,
        cursor: String? = null,
        size: Int? = null,
    ): ExploreResult

    /** 템플릿 복제 */
    suspend fun clone(challengeId: String): DraftResult.Ok
}
