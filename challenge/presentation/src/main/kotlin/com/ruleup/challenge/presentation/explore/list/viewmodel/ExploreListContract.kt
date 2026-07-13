package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ExploreListIntent : MviIntent {
    /** 화면 진입. 라우트 인자(카테고리 초기 필터·초기 정렬)로 첫 페이지를 조회한다. */
    data class Load(
        val category: String?,
        val sort: String?,
    ) : ExploreListIntent

    /** 목록 하단 근접 시 다음 커서 페이지 로드. */
    data object LoadMore : ExploreListIntent

    /** 필터 시트 "결과 보기" 확정 → 필터 적용 + 첫 페이지 재조회. */
    data class ApplyFilter(
        val filter: ExploreFilter,
    ) : ExploreListIntent

    /**
     * 필터 시트에서 조건 변경 시 "결과 보기 · N개" 미리보기 카운트 조회.
     * 목록 상태는 바꾸지 않는다(확정은 [ApplyFilter]).
     */
    data class PreviewFilter(
        val filter: ExploreFilter,
    ) : ExploreListIntent

    /** 정렬 시트에서 정렬 선택 → 첫 페이지 재조회. */
    data class SelectSort(
        val sort: ExploreSort,
    ) : ExploreListIntent

    /** 카드 → 챌린지 상세(공개 상세). */
    data class OpenChallenge(
        val challengeId: String,
    ) : ExploreListIntent

    data object Back : ExploreListIntent
}

data class ExploreListState(
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val filter: ExploreFilter,
    val sort: ExploreSort,
    val totalCount: Int,
    val challenges: List<ExploreChallenge>,
    // null 이면 마지막 페이지(더 불러올 것 없음)
    val nextCursor: String?,
    val errorMessage: String?,
    // 필터 시트 "결과 보기 · N개". null 이면 집계 중(버튼엔 카운트 생략)
    val previewCount: Int?,
) : UiState {
    val canLoadMore: Boolean
        get() = nextCursor != null && !isLoading && !isLoadingMore

    companion object {
        val initial =
            ExploreListState(
                isLoading = true,
                isLoadingMore = false,
                filter = ExploreFilter.none,
                // API 기본 정렬 = 인기순
                sort = ExploreSort.TRENDING,
                totalCount = 0,
                challenges = emptyList(),
                nextCursor = null,
                errorMessage = null,
                previewCount = null,
            )
    }
}

sealed interface ExploreListReducerEvent : ReducerEvent {
    /** 첫 페이지 조회 시작(필터/정렬 확정 포함). */
    data class Loading(
        val filter: ExploreFilter,
        val sort: ExploreSort,
    ) : ExploreListReducerEvent

    data class FirstPageLoaded(
        val totalCount: Int,
        val challenges: List<ExploreChallenge>,
        val nextCursor: String?,
    ) : ExploreListReducerEvent

    data object LoadingMore : ExploreListReducerEvent

    data class MorePageLoaded(
        val challenges: List<ExploreChallenge>,
        val nextCursor: String?,
    ) : ExploreListReducerEvent

    data class Failed(
        val message: String,
    ) : ExploreListReducerEvent

    data object PreviewCounting : ExploreListReducerEvent

    data class PreviewCounted(
        val count: Int,
    ) : ExploreListReducerEvent
}
