package com.ruleup.profile.presentation.tier.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 티어 히스토리 ViewModel. 월말 스냅샷과 역대 최고만 그린다 —
 * 하락 사유는 정책상 표기하지 않으므로 서버도 내려주지 않는다.
 */
@HiltViewModel
class MyTierHistoryViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyTierHistoryIntent, MyTierHistoryState, MyTierHistoryReducerEvent, NoEffect>(
            MyTierHistoryState.initial,
        ) {
        override fun onIntent(intent: MyTierHistoryIntent) {
            when (intent) {
                MyTierHistoryIntent.Load -> load()
                MyTierHistoryIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: MyTierHistoryState,
            event: MyTierHistoryReducerEvent,
        ): MyTierHistoryState =
            when (event) {
                MyTierHistoryReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is MyTierHistoryReducerEvent.Loaded ->
                    state.copy(isLoading = false, history = event.history, errorMessage = null)

                is MyTierHistoryReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (currentState.history != null) return
            dispatch(MyTierHistoryReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { myPageRepository.getTierHistory() }
                    .onSuccess { dispatch(MyTierHistoryReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(MyTierHistoryReducerEvent.Failed(it.message ?: "히스토리를 불러오지 못했어요")) }
            }
        }
    }
