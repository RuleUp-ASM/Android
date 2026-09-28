package com.ruleup.profile.presentation.locked.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.notification.domain.navigation.NotificationCenterPage
import com.ruleup.onboarding.domain.auth.usecase.LogoutUseCase
import com.ruleup.profile.domain.navigation.MySanctionsPage
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.navigation.InquiryComposePage
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** 잠금 화면. */
@HiltViewModel
class AccountLockedViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val logoutUseCase: LogoutUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<AccountLockedIntent, AccountLockedState, AccountLockedReducerEvent, AccountLockedEffect>(
            AccountLockedState.initial,
        ) {
        override fun onIntent(intent: AccountLockedIntent) {
            when (intent) {
                AccountLockedIntent.Load -> load(force = false)
                AccountLockedIntent.Retry -> load(force = true)
                AccountLockedIntent.OpenHistory -> navigationHelper.navigateTo(MySanctionsPage)
                AccountLockedIntent.OpenNotifications -> navigationHelper.navigateTo(NotificationCenterPage)
                AccountLockedIntent.RequestReview ->
                    navigationHelper.navigateByRoute(InquiryComposePage(InquiryCategory.REPORT_SANCTION).toRoute())

                AccountLockedIntent.Logout -> logout()
            }
        }

        override fun reduce(
            state: AccountLockedState,
            event: AccountLockedReducerEvent,
        ): AccountLockedState =
            when (event) {
                AccountLockedReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null, isOffline = false)

                is AccountLockedReducerEvent.Loaded ->
                    state.copy(isLoading = false, sanction = event.sanction, errorMessage = null, isOffline = false)

                is AccountLockedReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message, isOffline = event.offline)
            }

        private fun load(force: Boolean) {
            if (!force && currentState.sanction != null) return
            dispatch(AccountLockedReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { accountRepository.getSanctions() }
                    .onSuccess { dispatch(AccountLockedReducerEvent.Loaded(it.activeSanction)) }
                    .onFailure {
                        dispatch(
                            AccountLockedReducerEvent.Failed(
                                message = it.userFacingMessage("제재 정보를 불러오지 못했어요"),
                                // 연결 문제면 전체 화면 재시도를 띄운다
                                offline = it is IOException,
                            ),
                        )
                    }
            }
        }

        /** 로그아웃하면 세션이 끊겨 스플래시가 다시 진입을 판정한다 */
        private fun logout() {
            viewModelScope.launch { runCatching { logoutUseCase() } }
        }
    }
