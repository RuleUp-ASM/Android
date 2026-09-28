package com.ruleup.profile.presentation.tier.viewmodel

import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyTierHistoryIntent : MviIntent {
    data object Load : MyTierHistoryIntent

    /** 목록 끝에 닿았다. */
    data object LoadMore : MyTierHistoryIntent

    data object Back : MyTierHistoryIntent
}

/** 이 화면은 원천이 둘이다 */
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

/** 일회성 이펙트 없음. */
typealias MyTierHistoryEffect = NoEffect
