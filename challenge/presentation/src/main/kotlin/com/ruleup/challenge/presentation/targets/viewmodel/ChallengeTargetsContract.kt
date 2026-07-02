package com.ruleup.challenge.presentation.targets.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ChallengeTargetsIntent : MviIntent {
    /** 선택한 대상 앱 패키지명을 저장하고 종료. */
    data class Save(
        val challengeId: String,
        val packages: List<String>,
    ) : ChallengeTargetsIntent

    data object Back : ChallengeTargetsIntent
}

data class ChallengeTargetsState(
    val isSaving: Boolean = false,
) : UiState {
    companion object {
        val initial = ChallengeTargetsState()
    }
}

sealed interface ChallengeTargetsReducerEvent : ReducerEvent {
    data object Saving : ChallengeTargetsReducerEvent

    data object Finished : ChallengeTargetsReducerEvent
}

sealed interface ChallengeTargetsEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : ChallengeTargetsEffect
}
