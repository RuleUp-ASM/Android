package com.ruleup.profile.presentation.sanctions.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 제재 통지·이력 ViewModel. */
@HiltViewModel
class SanctionsViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<SanctionsIntent, SanctionsState, SanctionsReducerEvent, NoEffect>(
            SanctionsState.initial,
        ) {
        override fun onIntent(intent: SanctionsIntent) {
            when (intent) {
                SanctionsIntent.Load -> load()
                SanctionsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: SanctionsState,
            event: SanctionsReducerEvent,
        ): SanctionsState =
            when (event) {
                SanctionsReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is SanctionsReducerEvent.Loaded ->
                    state.copy(isLoading = false, history = event.history, errorMessage = null)

                is SanctionsReducerEvent.Failed -> state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (currentState.history != null) return
            dispatch(SanctionsReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { accountRepository.getSanctions() }
                    .onSuccess { dispatch(SanctionsReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(SanctionsReducerEvent.Failed(it.userFacingMessage("제재 이력을 불러오지 못했어요"))) }
            }
        }
    }
