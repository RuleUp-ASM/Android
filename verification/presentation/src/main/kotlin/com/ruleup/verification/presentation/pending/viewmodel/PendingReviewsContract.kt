package com.ruleup.verification.presentation.pending.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.ObjectionDecision
import com.ruleup.verification.domain.entity.PendingReviews

sealed interface PendingReviewsIntent : MviIntent {
    data class Load(
        val challengeId: String,
    ) : PendingReviewsIntent

    /** 이의 제기 승인/기각. */
    data class Decide(
        val objectionId: String,
        val decision: ObjectionDecision,
    ) : PendingReviewsIntent

    data object Back : PendingReviewsIntent
}

data class PendingReviewsState(
    val isLoading: Boolean = true,
    val reviews: PendingReviews? = null,
    val error: String? = null,
    // 승인/기각 요청 중(버튼 중복 탭 방지).
    val isDeciding: Boolean = false,
) : UiState {
    companion object {
        val initial = PendingReviewsState()
    }
}

sealed interface PendingReviewsReducerEvent : ReducerEvent {
    data object Loading : PendingReviewsReducerEvent

    data class Loaded(
        val reviews: PendingReviews,
    ) : PendingReviewsReducerEvent

    data class Failed(
        val message: String,
    ) : PendingReviewsReducerEvent

    data class Deciding(
        val deciding: Boolean,
    ) : PendingReviewsReducerEvent
}

sealed interface PendingReviewsEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : PendingReviewsEffect
}
