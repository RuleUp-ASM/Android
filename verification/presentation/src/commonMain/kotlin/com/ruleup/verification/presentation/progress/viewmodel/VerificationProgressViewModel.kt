package com.ruleup.verification.presentation.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.VerificationDetailPage
import com.ruleup.verification.domain.usecase.ObserveProgressUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

/**
 * 내 챌린지 진행률 일괄(명세 3.2·§6.1). 진입 시 1회 조회(백그라운드 sync 가 이미 갱신한 값).
 * 카드 탭 → 검증 결과 상세로 이동.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class VerificationProgressViewModel
    constructor(
        private val observeProgressUseCase: ObserveProgressUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<VerificationProgressIntent, VerificationProgressState, VerificationProgressReducerEvent, NoEffect>(
            VerificationProgressState.initial,
        ) {
        override fun onIntent(intent: VerificationProgressIntent) {
            when (intent) {
                VerificationProgressIntent.Load -> load()
                is VerificationProgressIntent.OpenDetail ->
                    navigationHelper.navigateTo(VerificationDetailPage(intent.challengeId))
            }
        }

        override fun reduce(
            state: VerificationProgressState,
            event: VerificationProgressReducerEvent,
        ): VerificationProgressState =
            when (event) {
                VerificationProgressReducerEvent.Loading -> state.copy(isLoading = true, error = null)
                is VerificationProgressReducerEvent.Loaded -> state.copy(isLoading = false, challenges = event.challenges, error = null)
                is VerificationProgressReducerEvent.Failed -> state.copy(isLoading = false, error = event.message)
            }

        private fun load() {
            if (currentState.isLoading) return
            viewModelScope.launch {
                dispatch(VerificationProgressReducerEvent.Loading)
                runCatching { observeProgressUseCase() }
                    .onSuccess { dispatch(VerificationProgressReducerEvent.Loaded(it.challenges)) }
                    .onFailure { dispatch(VerificationProgressReducerEvent.Failed(it.message ?: "진행률을 불러오지 못했어요")) }
            }
        }
    }
