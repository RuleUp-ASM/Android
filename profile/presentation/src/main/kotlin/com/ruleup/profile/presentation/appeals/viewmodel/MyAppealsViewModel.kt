package com.ruleup.profile.presentation.appeals.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 이의 내역 ViewModel (명세 GET /users/me/appeals).
 *
 * 접수된 이의는 즉시 인용되므로 계류·기각 상태가 없고, 형식 미달은 접수 자체가 안 되어 이력에도
 * 남지 않는다 — 그래서 상태 필터도 페이지네이션도 없는 단순 목록이다.
 */
@HiltViewModel
class MyAppealsViewModel
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyAppealsIntent, MyAppealsState, MyAppealsReducerEvent, NoEffect>(
            MyAppealsState.initial,
        ) {
        override fun onIntent(intent: MyAppealsIntent) {
            when (intent) {
                MyAppealsIntent.Load -> load(force = false)
                MyAppealsIntent.Retry -> load(force = true)
                MyAppealsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: MyAppealsState,
            event: MyAppealsReducerEvent,
        ): MyAppealsState =
            when (event) {
                MyAppealsReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is MyAppealsReducerEvent.Loaded ->
                    state.copy(isLoading = false, history = event.history, errorMessage = null)

                is MyAppealsReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load(force: Boolean) {
            if (!force && currentState.history.isNotEmpty()) return
            dispatch(MyAppealsReducerEvent.Loading)
            viewModelScope.launch {
                // 1회 자동 재시도(프론트엔드 테크스펙 4-6) — 일시적 실패로 빈 화면을 보여주지 않는다.
                runCatching { verificationRepository.getMyAppeals() }
                    .recoverCatching { verificationRepository.getMyAppeals() }
                    .onSuccess { dispatch(MyAppealsReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(MyAppealsReducerEvent.Failed(it.message ?: "이의 내역을 불러오지 못했어요")) }
            }
        }
    }
