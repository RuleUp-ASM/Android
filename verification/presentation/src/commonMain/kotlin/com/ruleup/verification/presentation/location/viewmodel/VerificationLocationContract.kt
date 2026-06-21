package com.ruleup.verification.presentation.location.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface VerificationLocationIntent : MviIntent {
    /** 지도 핀 확정(명세 §5.3 결과 {lat,lng,radiusM,label}). */
    data class Confirm(
        val challengeMemberId: String,
        val lat: Double,
        val lng: Double,
        val radiusM: Float,
        val dwellMinutes: Int,
        // 표시·서버 전송용(로컬 지오펜스에는 미사용).
        val label: String,
    ) : VerificationLocationIntent
}

data class VerificationLocationState(
    val isBinding: Boolean = false,
) : UiState {
    companion object {
        val initial = VerificationLocationState()
    }
}

sealed interface VerificationLocationReducerEvent : ReducerEvent {
    data object Binding : VerificationLocationReducerEvent

    data object Finished : VerificationLocationReducerEvent
}

sealed interface VerificationLocationEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : VerificationLocationEffect
}
