package com.ruleup.challenge.presentation.explore.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.domain.entity.category.Category
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ExploreIntent : MviIntent {
    /** 화면 진입 — 인기와 카테고리를 병렬로 조회한다. */
    data object Load : ExploreIntent

    /** 인기 섹션만 재시도. */
    data object RetryTrending : ExploreIntent

    /** 카테고리 그리드만 재시도. */
    data object RetryCategories : ExploreIntent

    /** 인기 항목 → 챌린지 공개 상세. */
    data class OpenChallenge(
        val challengeId: String,
    ) : ExploreIntent

    /** 실시간 인기 "전체 ›" → 둘러보기(인기순). */
    data object OpenTrendingAll : ExploreIntent

    /** 카테고리 탐색 "전체 ›" → 둘러보기(필터 없음). */
    data object OpenCategoryAll : ExploreIntent

    /** 카테고리 타일 → 둘러보기(해당 카테고리 프리필). */
    data class OpenCategory(
        val category: Category,
    ) : ExploreIntent

    /** 하단 탭 "홈". */
    data object OpenHome : ExploreIntent

    /** 하단 탭 "마이". */
    data object OpenMy : ExploreIntent

    /** 하단 탭 가운데 생성 버튼. */
    data object CreateChallenge : ExploreIntent
}

/**
 * 탐색 메인 상태.
 *
 * 인기와 카테고리는 **서로 독립적으로** 로딩·실패한다 — 한쪽이 실패해도 다른 쪽을 막지 않기 위해
 * 공통 `isLoading`/`errorMessage` 를 두지 않는다(프론트 스펙 4-4).
 */
data class ExploreState(
    val isTrendingLoading: Boolean,
    // 서버 산정 순서 그대로의 상위 N
    val trending: List<TrendingChallenge>,
    // 순위 계산 기준 시각 — 최대 1시간 지연된 스냅샷이다
    val calculatedAt: String?,
    val trendingFailed: Boolean,
    val isCategoriesLoading: Boolean,
    val categories: List<ChallengeCategoryCount>,
    val categoriesFailed: Boolean,
) : UiState {
    /**
     * 인기 섹션을 아예 숨길지. 초기 상태(`[]`)이거나 실패했으면 숨긴다 —
     * 빈 카드를 남겨두면 "인기 챌린지가 없는 서비스"처럼 보인다.
     */
    val hideTrendingSection: Boolean
        get() = !isTrendingLoading && (trending.isEmpty() || trendingFailed)

    companion object {
        val initial =
            ExploreState(
                isTrendingLoading = true,
                trending = emptyList(),
                calculatedAt = null,
                trendingFailed = false,
                isCategoriesLoading = true,
                categories = emptyList(),
                categoriesFailed = false,
            )
    }
}

sealed interface ExploreReducerEvent : ReducerEvent {
    data object TrendingLoading : ExploreReducerEvent

    data class TrendingLoaded(
        val items: List<TrendingChallenge>,
        val calculatedAt: String?,
    ) : ExploreReducerEvent

    data object TrendingFailed : ExploreReducerEvent

    data object CategoriesLoading : ExploreReducerEvent

    data class CategoriesLoaded(
        val categories: List<ChallengeCategoryCount>,
    ) : ExploreReducerEvent

    data object CategoriesFailed : ExploreReducerEvent
}
