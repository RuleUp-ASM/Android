package com.ruleup.verification.presentation.progress.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.navigation.VerificationDetailPage
import com.ruleup.verification.domain.usecase.ObserveProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 내 챌린지 진행률 일괄(명세 3.2·§6.1). 진입 시 1회 조회(백그라운드 sync 가 이미 갱신한 값).
 * 카드 탭 → 검증 결과 상세로 이동.
 */
@HiltViewModel
class VerificationProgressViewModel
    @Inject
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
