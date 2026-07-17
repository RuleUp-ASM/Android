package com.ruleup.profile.presentation.history.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.usecase.GetReputationHistoryUseCase
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 평판 히스토리 ViewModel. 전체 반환(상한 50건) — 페이지네이션 없음. */
@HiltViewModel
class ReputationHistoryViewModel
    @Inject
    constructor(
        private val getReputationHistoryUseCase: GetReputationHistoryUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ReputationHistoryIntent, ReputationHistoryState, ReputationHistoryReducerEvent, NoEffect>(
            ReputationHistoryState.initial,
        ) {
        override fun onIntent(intent: ReputationHistoryIntent) {
            when (intent) {
                ReputationHistoryIntent.Load -> load()
                ReputationHistoryIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ReputationHistoryState,
            event: ReputationHistoryReducerEvent,
        ): ReputationHistoryState =
            when (event) {
                ReputationHistoryReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is ReputationHistoryReducerEvent.Loaded ->
                    state.copy(isLoading = false, history = event.history, errorMessage = null)

                is ReputationHistoryReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (currentState.history != null) return
            dispatch(ReputationHistoryReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { getReputationHistoryUseCase() }
                    .onSuccess { dispatch(ReputationHistoryReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(ReputationHistoryReducerEvent.Failed(it.message ?: "히스토리를 불러오지 못했어요")) }
            }
        }
    }
