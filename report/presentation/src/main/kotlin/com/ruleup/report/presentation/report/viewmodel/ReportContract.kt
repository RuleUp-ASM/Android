package com.ruleup.report.presentation.report.viewmodel

import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ReportIntent : MviIntent {
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
        fun initial(
            targetName: String,
            reasons: List<ReportReason>,
        ) = ReportState(
            targetName = targetName,
            reasons = reasons,
            selected = null,
            isSubmitting = false,
            done = null,
        )
    }
}

sealed interface ReportReducerEvent : ReducerEvent {
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
