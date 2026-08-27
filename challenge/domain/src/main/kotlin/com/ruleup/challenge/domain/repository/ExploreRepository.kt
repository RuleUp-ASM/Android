package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.domain.entity.category.Category

/**
 * 챌린지 탐색. 랭킹·집계는 서버 역정규화 값을 그대로 신뢰하고 클라이언트가 재계산하지 않는다.
 *
 * 탐색 API 는 전부 **로그인 필수**다 — `eligible`·`joinable`·신고 제외가 사용자 컨텍스트에 의존한다.
 */
interface ExploreRepository {
    /**
     * 실시간 인기 (명세 GET /challenges/trending). 서버가 Top 20 을 순위와 함께 준다.
     * 1시간 주기 갱신이라 `calculatedAt` 이 최대 1시간 지연된다(정책 2026-08-11 변경).
     * [category] 를 주면 카테고리별 인기가 온다.
     */
    suspend fun getTrending(category: Category? = null): TrendingSnapshot

    /** 카테고리 그리드 12종 + 진행 중 공개 그룹 방 수 (명세 GET /challenge-categories). 10분 캐시. */
    suspend fun getCategories(): List<ChallengeCategoryCount>

    /**
     * 둘러보기 (명세 GET /challenges/explore).
     * 처리 순서는 ① 노출 제외 → ② 필터(AND) → ③ 정렬이고, 페이징은 커서다.
     *
     * [cursor] 는 이전 응답의 `nextCursor`(불투명 문자열)이며 첫 페이지는 null 이다. 손상·만료 커서는
     * [com.ruleup.challenge.domain.entity.CursorInvalidException] 으로 올라오므로 첫 페이지부터 다시 받는다.
     */
    suspend fun explore(
        filter: ExploreFilter = ExploreFilter.none,
        sort: ExploreSort = ExploreSort.default,
        cursor: String? = null,
        size: Int? = null,
    ): ExploreResult

    /**
     * 템플릿 복제 (명세 POST /challenges/{id}/clone) — 대상 방의 설정을 프리필한 초안을 만든다.
     *
     * 응답 초안은 생성 모듈의 draft 와 **동일 스키마**라 확인 화면·생성 API 를 그대로 재사용한다.
     * 공개 그룹만 복제할 수 있고, 그 외에는
     * [com.ruleup.challenge.domain.entity.ChallengeNotCloneableException] 이 던져진다.
     */
    suspend fun clone(challengeId: String): DraftResult.Ok
}
