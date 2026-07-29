package com.ruleup.challenge.presentation.explore.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.RoutineRecommendation
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.domain.entity.category.Category
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ExploreIntent : MviIntent {
    /** 화면 진입 시 실시간 인기 + 카테고리 수 조회. */
    data object Load : ExploreIntent

    /** 인기 랭킹 항목 → 챌린지 상세(공개 상세). */
    data class OpenChallenge(
        val challengeId: String,
    ) : ExploreIntent

    /** 실시간 인기 "전체 ›" → 둘러보기(인기순). */
    data object OpenTrendingAll : ExploreIntent

    /** 카테고리 탐색 "전체 ›" → 둘러보기(필터 없음). */
    data object OpenCategoryAll : ExploreIntent

    /** 카테고리 카드 → 둘러보기(카테고리 필터). */
    data class OpenCategory(
        val category: Category,
    ) : ExploreIntent

    /** 하단 탭 "홈". */
    data object OpenHome : ExploreIntent

    /** 하단 탭 "마이". */
    data object OpenMy : ExploreIntent
}

data class ExploreState(
    val isLoading: Boolean,
    // 서버 산정 순서 그대로의 실시간 인기(상위 N)
    val trending: List<TrendingChallenge>,
    val categories: List<ChallengeCategoryCount>,
    val errorMessage: String?,
    // 관심사+세그먼트 기반 추천 루틴(최대 3). 비어 있으면 섹션을 숨긴다.
    val recommendedRoutines: List<RoutineRecommendation> = emptyList(),
) : UiState {
    companion object {
        val initial =
            ExploreState(
                isLoading = true,
                trending = emptyList(),
                categories = emptyList(),
                errorMessage = null,
            )
    }
}

sealed interface ExploreReducerEvent : ReducerEvent {
    data object Loading : ExploreReducerEvent

    data class Loaded(
        val trending: List<TrendingChallenge>,
        val categories: List<ChallengeCategoryCount>,
        val recommendedRoutines: List<RoutineRecommendation>,
    ) : ExploreReducerEvent

    data class Failed(
        val message: String,
    ) : ExploreReducerEvent
}
