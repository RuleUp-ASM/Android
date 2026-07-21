package com.ruleup.challenge.presentation.targets.viewmodel

import com.ruleup.entity.challenge.BoundScreenApp
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ChallengeTargetsIntent : MviIntent {
    /** 진입 시 서버에 바인딩된 대상 앱을 조회해 이전 선택을 복원한다. */
    data class Load(
        val challengeId: String,
    ) : ChallengeTargetsIntent

    /** 선택한 대상 앱({packageName, appName})을 서버에 저장하고 종료. */
    data class Save(
        val challengeId: String,
        val apps: List<BoundScreenApp>,
    ) : ChallengeTargetsIntent

    data object Back : ChallengeTargetsIntent
}

data class ChallengeTargetsState(
    val isSaving: Boolean = false,
    // 서버에서 복원한(이전 바인딩) 대상 앱 패키지명 — 진입 시 선택 상태 시드용.
    val restoredPackages: Set<String> = emptySet(),
) : UiState {
    companion object {
        val initial = ChallengeTargetsState()
    }
}

sealed interface ChallengeTargetsReducerEvent : ReducerEvent {
    data class Restored(
        val packages: Set<String>,
    ) : ChallengeTargetsReducerEvent

    data object Saving : ChallengeTargetsReducerEvent

    data object Finished : ChallengeTargetsReducerEvent
}

sealed interface ChallengeTargetsEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : ChallengeTargetsEffect
}
