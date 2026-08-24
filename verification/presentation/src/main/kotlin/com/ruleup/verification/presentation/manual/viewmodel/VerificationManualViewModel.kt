package com.ruleup.verification.presentation.manual.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.InvalidTargetDateException
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface VerificationManualIntent : MviIntent {
    /** 수동 방의 자체 체크. [note] 는 기록용 메모라 서버가 검증하지 않는다. */
    data class Submit(
        val challengeId: String,
        val note: String? = null,
    ) : VerificationManualIntent
}

data class VerificationManualState(
    val isSubmitting: Boolean = false,
) : UiState {
    companion object {
        val initial = VerificationManualState()
    }
}

sealed interface VerificationManualReducerEvent : ReducerEvent {
    data object Submitting : VerificationManualReducerEvent

    data object Finished : VerificationManualReducerEvent
}

sealed interface VerificationManualEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : VerificationManualEffect
}

/**
 * 수동 인증 제출(명세: POST /challenges/{id}/verifications). **수동 방 전용**이다 —
 * 자동 방의 실패 구제는 이의 제기가 담당하므로 보조 수동 버튼을 두지 않는다.
 *
 * 당일 중복(409)은 오류가 아니라 "이미 인증됨"으로 안내하고, 자정을 넘겨 기한이 지난 경우(400)는
 * 왜 막혔는지 따로 말해 준다.
 */
@HiltViewModel
class VerificationManualViewModel
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<VerificationManualIntent, VerificationManualState, VerificationManualReducerEvent, VerificationManualEffect>(
            VerificationManualState.initial,
        ) {
        override fun onIntent(intent: VerificationManualIntent) {
            when (intent) {
                is VerificationManualIntent.Submit -> submit(intent)
            }
        }

        override fun reduce(
            state: VerificationManualState,
            event: VerificationManualReducerEvent,
        ): VerificationManualState =
            when (event) {
                VerificationManualReducerEvent.Submitting -> state.copy(isSubmitting = true)
                VerificationManualReducerEvent.Finished -> state.copy(isSubmitting = false)
            }

        private fun submit(intent: VerificationManualIntent.Submit) {
            if (currentState.isSubmitting) return
            viewModelScope.launch {
                dispatch(VerificationManualReducerEvent.Submitting)
                runCatching {
                    verificationRepository.submitManual(
                        challengeId = intent.challengeId,
                        note = intent.note,
                    )
                }.onSuccess {
                    dispatch(VerificationManualReducerEvent.Finished)
                    // 제출 즉시 확정이라 잠정 상태 안내가 없다.
                    emitEffect(VerificationManualEffect.ShowMessage("오늘 인증을 제출했어요"))
                    navigationHelper.navigateToBack()
                }.onFailure { error ->
                    dispatch(VerificationManualReducerEvent.Finished)
                    val message =
                        when (error) {
                            // 409 는 오류가 아니라 안내다.
                            is AlreadyVerifiedException -> "오늘은 이미 인증했어요"
                            // 화면을 열어 둔 채 자정을 넘긴 경우 — 방금까지 되던 버튼이 왜 막혔는지 말해 준다.
                            is InvalidTargetDateException -> "오늘이 지나 체크할 수 없어요"
                            else -> error.message ?: "제출에 실패했어요"
                        }
                    emitEffect(VerificationManualEffect.ShowMessage(message))
                }
            }
        }
    }
