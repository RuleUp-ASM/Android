package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ChallengeDetailIntent : MviIntent {
    /** 화면 진입 시 상세 조회. */
    data class Load(
        val challengeId: String,
    ) : ChallengeDetailIntent

    /** 권한이 모두 확보된 뒤 참여 진행(좌표 바인딩 화면으로 이동). */
    data object Proceed : ChallengeDetailIntent

    data object Back : ChallengeDetailIntent
}

data class ChallengeDetailState(
    val challengeId: String,
    val isLoading: Boolean,
    val detail: ChallengeDetail?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            ChallengeDetailState(
                challengeId = "",
                isLoading = true,
                detail = null,
                errorMessage = null,
            )
    }
}

sealed interface ChallengeDetailReducerEvent : ReducerEvent {
    data class Loading(
        val challengeId: String,
    ) : ChallengeDetailReducerEvent

    data class Loaded(
        val detail: ChallengeDetail,
    ) : ChallengeDetailReducerEvent

    data class Failed(
        val message: String,
    ) : ChallengeDetailReducerEvent
}
