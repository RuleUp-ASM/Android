package com.ruleup.profile.presentation.temperature.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.navigation.ReputationHistoryPage
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 매너 온도 상세 ViewModel. 온도는 일별 배치 계산(서버) — 화면은 조회·표시만 한다.
 * 변동 로그는 서버 고정 최근 10건이라 "더 불러오기"가 없다.
 */
@HiltViewModel
class MyTemperatureViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyTemperatureIntent, MyTemperatureState, MyTemperatureReducerEvent, NoEffect>(
            MyTemperatureState.initial,
        ) {
        override fun onIntent(intent: MyTemperatureIntent) {
            when (intent) {
                MyTemperatureIntent.Load -> load()
                MyTemperatureIntent.OpenHistory -> navigationHelper.navigateByRoute(ReputationHistoryPage.toRoute())
                MyTemperatureIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: MyTemperatureState,
            event: MyTemperatureReducerEvent,
        ): MyTemperatureState =
            when (event) {
                MyTemperatureReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is MyTemperatureReducerEvent.Loaded ->
                    state.copy(isLoading = false, detail = event.detail, errorMessage = null)

                is MyTemperatureReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (currentState.detail != null) return
            dispatch(MyTemperatureReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { myPageRepository.getReputation() }
                    .onSuccess { dispatch(MyTemperatureReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(MyTemperatureReducerEvent.Failed(it.message ?: "온도 정보를 불러오지 못했어요")) }
            }
        }
    }
