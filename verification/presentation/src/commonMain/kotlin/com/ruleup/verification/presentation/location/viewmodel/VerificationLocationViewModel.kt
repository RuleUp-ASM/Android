package com.ruleup.verification.presentation.location.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.usecase.BindLocationUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

/**
 * 지도 핀 → 멤버 좌표 지오펜스 바인딩(명세 §5). 확정 시 [BindLocationUseCase] 로 등록(재등록 포함)하고
 * 이전 화면으로 돌아간다. 미설정/권한 등 엣지케이스는 화면(LocationPickerContent)에서 처리한다.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class VerificationLocationViewModel
    constructor(
        private val bindLocationUseCase: BindLocationUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<VerificationLocationIntent, VerificationLocationState, VerificationLocationReducerEvent, VerificationLocationEffect>(
            VerificationLocationState.initial,
        ) {
        override fun onIntent(intent: VerificationLocationIntent) {
            when (intent) {
                is VerificationLocationIntent.Confirm -> bind(intent)
            }
        }

        override fun reduce(
            state: VerificationLocationState,
            event: VerificationLocationReducerEvent,
        ): VerificationLocationState =
            when (event) {
                VerificationLocationReducerEvent.Binding -> state.copy(isBinding = true)
                VerificationLocationReducerEvent.Finished -> state.copy(isBinding = false)
            }

        private fun bind(intent: VerificationLocationIntent.Confirm) {
            if (currentState.isBinding) return
            viewModelScope.launch {
                dispatch(VerificationLocationReducerEvent.Binding)
                runCatching {
                    bindLocationUseCase(
                        challengeMemberId = intent.challengeMemberId,
                        lat = intent.lat,
                        lng = intent.lng,
                        radiusM = intent.radiusM,
                        dwellMinutes = intent.dwellMinutes,
                    )
                }.onSuccess {
                    dispatch(VerificationLocationReducerEvent.Finished)
                    emitEffect(VerificationLocationEffect.ShowMessage("장소가 등록됐어요"))
                    navigationHelper.navigateToBack()
                }.onFailure { error ->
                    dispatch(VerificationLocationReducerEvent.Finished)
                    emitEffect(VerificationLocationEffect.ShowMessage(error.message ?: "장소 등록에 실패했어요"))
                }
            }
        }
    }
