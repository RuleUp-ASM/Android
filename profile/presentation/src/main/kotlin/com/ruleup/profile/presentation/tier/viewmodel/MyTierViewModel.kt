package com.ruleup.profile.presentation.tier.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.navigation.MyTierHistoryPage
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 내 티어 상세 ViewModel. 승·강등 판정은 서버가 하고 화면은 조회·표시만 한다.
 * 최근 변동은 서버 고정 10건이라 "더 불러오기"가 없다.
 */
@HiltViewModel
class MyTierViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyTierIntent, MyTierState, MyTierReducerEvent, NoEffect>(
            MyTierState.initial,
        ) {
        override fun onIntent(intent: MyTierIntent) {
            when (intent) {
                MyTierIntent.Load -> load()
                MyTierIntent.OpenHistory -> navigationHelper.navigateByRoute(MyTierHistoryPage.toRoute())
                MyTierIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: MyTierState,
            event: MyTierReducerEvent,
        ): MyTierState =
            when (event) {
                MyTierReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is MyTierReducerEvent.Loaded ->
                    state.copy(isLoading = false, tier = event.tier, errorMessage = null)

                is MyTierReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (currentState.tier != null) return
            dispatch(MyTierReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { myPageRepository.getTier() }
                    .onSuccess { dispatch(MyTierReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(MyTierReducerEvent.Failed(it.message ?: "티어 정보를 불러오지 못했어요")) }
            }
        }
    }
