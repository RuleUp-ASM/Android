package com.ruleup.verification.presentation.manual.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.usecase.SubmitManualUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

sealed interface VerificationManualIntent : MviIntent {
    /** MVP 는 SELF_CHECK 우선(명세 §6.5). PHOTO 는 화면에서 비강조. */
    data class Submit(
        val challengeId: String,
        val method: ManualMethod,
        val imageUrl: String? = null,
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
 * 수동 인증 제출(명세 §6.5, VF-04). SELF_CHECK 우선, PHOTO 는 비강조(이미지 업로드는 기존 챌린지 패턴 재사용).
 * 당일 중복(409)은 오류가 아니라 "이미 인증됨"으로 안내한다.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class VerificationManualViewModel
    constructor(
        private val submitManualUseCase: SubmitManualUseCase,
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
                    submitManualUseCase(
                        challengeId = intent.challengeId,
                        method = intent.method,
                        imageUrl = intent.imageUrl,
                    )
                }.onSuccess {
                    dispatch(VerificationManualReducerEvent.Finished)
                    emitEffect(VerificationManualEffect.ShowMessage("오늘 인증을 제출했어요"))
                    navigationHelper.navigateToBack()
                }.onFailure { error ->
                    dispatch(VerificationManualReducerEvent.Finished)
                    val message =
                        when (error) {
                            // 409 는 오류가 아니라 안내(명세 §6.5).
                            is AlreadyVerifiedException -> "오늘은 이미 인증했어요"
                            else -> error.message ?: "제출에 실패했어요"
                        }
                    emitEffect(VerificationManualEffect.ShowMessage(message))
                }
            }
        }
    }
