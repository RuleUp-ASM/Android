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
import com.ruleup.verification.domain.entity.FallbackLimitExceededException
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.VerifiedVia
import com.ruleup.verification.domain.usecase.SubmitManualUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

sealed interface VerificationManualIntent : MviIntent {
    /**
     * MVP 는 SELF_CHECK 우선(명세 §6.5). PHOTO 는 화면에서 비강조.
     * [asFallback]=true 면 자동 챌린지의 예비 폴백 제출(주1회·잠정성공·이의윈도우, §9.2).
     */
    data class Submit(
        val challengeId: String,
        val method: ManualMethod,
        val imageUrl: String? = null,
        val asFallback: Boolean = false,
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
                        asFallback = intent.asFallback,
                    )
                }.onSuccess { result ->
                    dispatch(VerificationManualReducerEvent.Finished)
                    // 예비 폴백은 잠정 성공(이의 윈도우 후 확정) — 정규 수동과 안내를 구분한다(명세 §9.2).
                    val message =
                        if (result.verifiedVia == VerifiedVia.MANUAL_FALLBACK) {
                            "오늘은 수동으로 잠정 인증했어요. 이의가 없으면 확정돼요"
                        } else {
                            "오늘 인증을 제출했어요"
                        }
                    emitEffect(VerificationManualEffect.ShowMessage(message))
                    navigationHelper.navigateToBack()
                }.onFailure { error ->
                    dispatch(VerificationManualReducerEvent.Finished)
                    val message =
                        when (error) {
                            // 409 는 오류가 아니라 안내(명세 §6.5).
                            is AlreadyVerifiedException -> "오늘은 이미 인증했어요"
                            // 예비 폴백 주1회 한도 초과(명세 §9.2).
                            is FallbackLimitExceededException -> "이번 주 수동 인증을 모두 사용했어요"
                            else -> error.message ?: "제출에 실패했어요"
                        }
                    emitEffect(VerificationManualEffect.ShowMessage(message))
                }
            }
        }
    }
