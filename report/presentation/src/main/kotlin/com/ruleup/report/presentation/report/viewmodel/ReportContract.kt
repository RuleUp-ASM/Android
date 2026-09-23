package com.ruleup.report.presentation.report.viewmodel

import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ReportIntent : MviIntent {
    /**
     * 화면 진입. **신고 대상을 화면이 넘긴다** — 호스트는 라우트 인자를 컴포저블 파라미터로 주지
     * `SavedStateHandle` 에 넣지 않아, ViewModel 이 거기서 읽으면 대상 이름이 비고 사유 목록도
     * 사용자/챌린지 구분 없이 챌린지용으로 굳는다.
     */
    data class Init(
        val userId: String?,
        val challengeId: String?,
        val targetName: String,
    ) : ReportIntent

    data class SelectReason(
        val reason: ReportReason,
    ) : ReportIntent

    data object Submit : ReportIntent

    data object Back : ReportIntent
}

data class ReportState(
    val targetName: String,
    val reasons: List<ReportReason>,
    val selected: ReportReason?,
    val isSubmitting: Boolean,
    val done: HiddenEffect?,
) : UiState {
    val canSubmit: Boolean get() = selected != null && !isSubmitting

    companion object {
        /** 대상이 정해지기 전 상태. [ReportIntent.Init] 이 곧바로 채운다. */
        val initial =
            ReportState(
                targetName = "",
                reasons = emptyList(),
                selected = null,
                isSubmitting = false,
                done = null,
            )
    }
}

sealed interface ReportReducerEvent : ReducerEvent {
    data class TargetResolved(
        val targetName: String,
        val reasons: List<ReportReason>,
    ) : ReportReducerEvent

    data class ReasonSelected(
        val reason: ReportReason,
    ) : ReportReducerEvent

    data class Submitting(
        val inProgress: Boolean,
    ) : ReportReducerEvent

    data class Done(
        val effect: HiddenEffect?,
    ) : ReportReducerEvent
}

sealed interface ReportEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : ReportEffect
}
