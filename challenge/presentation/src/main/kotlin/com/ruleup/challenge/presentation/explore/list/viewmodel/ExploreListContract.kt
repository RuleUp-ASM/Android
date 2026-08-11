package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ExploreListIntent : MviIntent {
    /** 화면 진입. 라우트 인자(카테고리 프리필·초기 정렬)로 첫 페이지를 조회한다. */
    data class Load(
        val category: String?,
        val sort: String?,
    ) : ExploreListIntent

    /** 목록 하단 근접 시 다음 커서 페이지 로드. */
    data object LoadMore : ExploreListIntent

    /**
     * 필터 시트 "적용" 확정 → 필터 적용 + 첫 페이지 재조회.
     * 시트에서는 선택만 하고 **적용을 눌렀을 때 1회 호출**한다 — 체크마다 부르지 않는다.
     */
    data class ApplyFilter(
        val filter: ExploreFilter,
    ) : ExploreListIntent

    /** 정렬 시트에서 정렬 선택 → 즉시 적용 후 첫 페이지 재조회. */
    data class SelectSort(
        val sort: ExploreSort,
    ) : ExploreListIntent

    /** 빈 결과에서 "티어 조건 끄기" — eligibleOnly 만 해제하고 재조회한다. */
    data object ClearEligibleOnly : ExploreListIntent

    /** 카드 → 챌린지 공개 상세. */
    data class OpenChallenge(
        val challengeId: String,
    ) : ExploreListIntent

    data object Back : ExploreListIntent
}

/**
 * 둘러보기 목록 상태.
 *
 * **전체 개수를 들고 있지 않다** — explore 계약에서 `totalCount` 가 제거됐다(무한 스크롤이라 불필요하고
 * 매 요청 COUNT 가 p95 목표에 부담). 화면도 "전체 N개" 표기를 그리지 않는다.
 */
data class ExploreListState(
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val filter: ExploreFilter,
    val sort: ExploreSort,
    val items: List<ExploreChallenge>,
    // null 이면 마지막 페이지
    val nextCursor: String?,
    val errorMessage: String?,
    // 다음 페이지만 실패한 상태. 기존 목록은 유지하고 하단에 "다시 불러오기"를 띄운다.
    val loadMoreFailed: Boolean,
) : UiState {
    val canLoadMore: Boolean
        get() = nextCursor != null && !isLoading && !isLoadingMore

    /** 결과가 0건일 때 어떤 문구를 보일지 — 사유별로 다음 행동이 다르다. */
    val emptyReason: EmptyReason?
        get() =
            when {
                isLoading || items.isNotEmpty() -> null
                // 지표 정렬은 표본 미달 방을 목록에서 아예 빼므로 "조건이 좁다"가 아니라 "기록이 없다"이다.
                sort.excludesLowSample -> EmptyReason.LOW_SAMPLE
                filter.eligibleOnly -> EmptyReason.TIER_FILTER
                filter.activeCount > 0 -> EmptyReason.FILTERED
                else -> EmptyReason.CATEGORY_EMPTY
            }

    companion object {
        val initial =
            ExploreListState(
                isLoading = true,
                isLoadingMore = false,
                filter = ExploreFilter.none,
                sort = ExploreSort.default,
                items = emptyList(),
                nextCursor = null,
                errorMessage = null,
                loadMoreFailed = false,
            )
    }
}

/** 빈 결과 사유. 문구와 CTA 가 각각 다르다. */
enum class EmptyReason {
    // 필터 결과 0건
    FILTERED,

    // 티어 컷이 켜져 있어 0건 — 완화를 우선 제안한다
    TIER_FILTER,

    // 지표 정렬인데 표본 미달로 전부 제외됨
    LOW_SAMPLE,

    // 이 카테고리에 방이 없음
    CATEGORY_EMPTY,
}

sealed interface ExploreListReducerEvent : ReducerEvent {
    /** 첫 페이지 조회 시작(필터·정렬 확정 포함). 커서는 버린다. */
    data class Loading(
        val filter: ExploreFilter,
        val sort: ExploreSort,
    ) : ExploreListReducerEvent

    data class FirstPageLoaded(
        val items: List<ExploreChallenge>,
        val nextCursor: String?,
    ) : ExploreListReducerEvent

    data object LoadingMore : ExploreListReducerEvent

    data class MorePageLoaded(
        val items: List<ExploreChallenge>,
        val nextCursor: String?,
    ) : ExploreListReducerEvent

    data object LoadMoreFailed : ExploreListReducerEvent

    data class Failed(
        val message: String,
    ) : ExploreListReducerEvent
}
