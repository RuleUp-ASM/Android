package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.entity.user.InterestCategory

/** 챌린지 탐색(홈 인기·카테고리·둘러보기) 조회. 랭킹/집계는 서버 역정규화 값을 그대로 신뢰한다. */
interface ExploreRepository {
    /**
     * 실시간 인기 챌린지 조회(명세: GET /challenges/trending). 필터 미적용, 신선도 SLA 10분.
     * [category] 를 주면 카테고리별 인기, null 이면 전체 인기.
     */
    suspend fun getTrending(category: InterestCategory? = null): List<TrendingChallenge>

    /** 카테고리별 진행 중 챌린지 수 조회(명세: GET /challenge-categories). */
    suspend fun getCategories(): List<ChallengeCategoryCount>

    /**
     * 챌린지 둘러보기(명세: GET /challenges/explore).
     * 처리 순서: 종료 제외 → 필터(AND) → 정렬 → 커서 페이지네이션.
     * [cursor] 는 이전 응답의 nextCursor(불투명 문자열), 첫 페이지는 null.
     */
    suspend fun explore(
        filter: ExploreFilter = ExploreFilter.none,
        sort: ExploreSort = ExploreSort.PARTICIPANTS,
        cursor: String? = null,
        size: Int? = null,
    ): ExploreResult
}
