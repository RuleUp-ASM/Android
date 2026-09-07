package com.ruleup.profile.presentation.tier.viewmodel

import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyTierHistoryIntent : MviIntent {
    data object Load : MyTierHistoryIntent

    /** 목록 끝에 닿았다. 다음 커서가 없으면 아무 일도 없다. */
    data object LoadMore : MyTierHistoryIntent

    data object Back : MyTierHistoryIntent
}

/**
 * 이 화면은 **원천이 둘**이다 — 그래프는 `/me/tier/history`(월말 스냅샷), 아래 목록은
 * `/me/tier/changes`(변동 건). 명세가 둘을 다른 API 로 나눈 이유가 페이징 단위가 달라서라
 * ([history] 는 한 번에 다 오고 [changes] 는 커서로 이어 붙는다) 상태도 따로 둔다.
 *
 * 한쪽이 실패해도 다른 쪽은 그린다. 그래프가 없다고 이력까지 감추면 사용자는 자기 점수가 왜
 * 움직였는지 볼 길이 사라진다.
 */
data class MyTierHistoryState(
    val isLoading: Boolean,
    val history: TierHistory?,
    val changes: List<ScoreChange>,
    val nextCursor: String?,
    val retentionDays: Int?,
    val isLoadingMore: Boolean,
    val errorMessage: String?,
) : UiState {
    /** 커서가 남아 있고 지금 읽는 중이 아니어야 더 읽는다. */
    val canLoadMore: Boolean
        get() = nextCursor != null && !isLoadingMore

    companion object {
        val initial =
            MyTierHistoryState(
                isLoading = true,
                history = null,
                changes = emptyList(),
                nextCursor = null,
                retentionDays = null,
                isLoadingMore = false,
                errorMessage = null,
            )
    }
}

sealed interface MyTierHistoryReducerEvent : ReducerEvent {
    data object Loading : MyTierHistoryReducerEvent

    data class Loaded(
        val history: TierHistory?,
        val changes: List<ScoreChange>,
        val nextCursor: String?,
        val retentionDays: Int?,
    ) : MyTierHistoryReducerEvent

    data object LoadingMore : MyTierHistoryReducerEvent

    data class MoreLoaded(
        val changes: List<ScoreChange>,
        val nextCursor: String?,
    ) : MyTierHistoryReducerEvent

    data object MoreFailed : MyTierHistoryReducerEvent

    data class Failed(
        val message: String,
    ) : MyTierHistoryReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyTierHistoryEffect = NoEffect
