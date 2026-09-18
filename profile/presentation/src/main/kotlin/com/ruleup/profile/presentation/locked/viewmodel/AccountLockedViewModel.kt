package com.ruleup.profile.presentation.locked.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.notification.domain.navigation.NotificationCenterPage
import com.ruleup.onboarding.domain.auth.usecase.LogoutUseCase
import com.ruleup.profile.domain.navigation.MySanctionsPage
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.navigation.InquiryComposePage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * 잠금 화면 (제재 정책 §5.3 · Figma `1465:37`·`1465:89`).
 *
 * 로그인 정지·영구 정지 계정이 로그인 직후 고정 진입한다. **여기서 갈 수 있는 곳은 셋뿐이다** —
 * 제재 이력, 알림함, CS 문의(재검토). 정책이 허용 행위를 그 둘로 못박았고 알림함은 잠금 화면에서
 * 열람할 수 있다고 명시한다.
 *
 * 제재 정보는 `GET /users/me/sanctions` 에서 온다 — 잠금 상태에서도 열리도록 게이트 화이트리스트에
 * 들어 있는 API다. 조회가 실패하면 사유·해제일을 못 보여 주지만 **로그아웃과 문의는 막지 않는다** —
 * 사용자가 상황을 다툴 경로까지 사라지면 안 된다.
 */
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
                                message = it.message ?: "제재 정보를 불러오지 못했어요",
                                // 연결 문제면 전체 화면 재시도를 띄운다 — 그 외 실패는 화면을 비우지 않는다.
                                offline = it is IOException,
                            ),
                        )
                    }
            }
        }

        /** 로그아웃하면 세션이 끊겨 스플래시가 다시 진입을 판정한다 — 이동은 그쪽이 맡는다. */
        private fun logout() {
            viewModelScope.launch { runCatching { logoutUseCase() } }
        }
    }
