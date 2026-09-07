package com.ruleup.profile.presentation.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.onboarding.domain.auth.usecase.LogoutUseCase
import com.ruleup.onboarding.domain.auth.usecase.WithdrawUseCase
import com.ruleup.profile.domain.navigation.MyAgreementsPage
import com.ruleup.profile.domain.navigation.MySanctionsPage
import com.ruleup.profile.domain.navigation.MyWatchingPage
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.report.domain.navigation.BlockListPage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 설정 허브 ViewModel (Figma 1134:2164).
 *
 * 뱃지에 쓸 두 값(재동의 대상 수·효력 중인 제재)만 미리 받는다. **둘 다 실패해도 화면은 뜬다** —
 * 설정 허브는 진입점 목록이라, 뱃지를 못 그린다고 로그아웃 경로까지 막으면 안 된다.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val logoutUseCase: LogoutUseCase,
        private val withdrawUseCase: WithdrawUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<SettingsIntent, SettingsState, SettingsReducerEvent, SettingsEffect>(
            SettingsState.initial,
        ) {
        override fun onIntent(intent: SettingsIntent) {
            when (intent) {
                SettingsIntent.Load -> load()
                SettingsIntent.OpenWatching -> navigationHelper.navigateTo(MyWatchingPage)
                SettingsIntent.OpenBlocks -> navigationHelper.navigateTo(BlockListPage)
                SettingsIntent.OpenAgreements -> navigationHelper.navigateTo(MyAgreementsPage)
                SettingsIntent.OpenSanctions -> navigationHelper.navigateTo(MySanctionsPage)

                SettingsIntent.OpenNotificationSettings ->
                    emitEffect(SettingsEffect.ShowMessage("알림 설정은 준비 중이에요"))

                SettingsIntent.ConfirmLogout -> dispatch(SettingsReducerEvent.DialogShown(SettingsDialog.LOGOUT))
                SettingsIntent.ConfirmWithdraw -> dispatch(SettingsReducerEvent.DialogShown(SettingsDialog.WITHDRAW))
                SettingsIntent.DismissDialog -> dispatch(SettingsReducerEvent.DialogDismissed)
                SettingsIntent.Logout -> logout()
                SettingsIntent.Withdraw -> withdraw()
                SettingsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: SettingsState,
            event: SettingsReducerEvent,
        ): SettingsState =
            when (event) {
                is SettingsReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        reconsentCount = event.reconsentCount,
                        hasActiveSanction = event.hasActiveSanction,
                    )

                SettingsReducerEvent.LoadFinished -> state.copy(isLoading = false)

                is SettingsReducerEvent.DialogShown -> state.copy(dialog = event.dialog)

                SettingsReducerEvent.DialogDismissed -> state.copy(dialog = null)

                is SettingsReducerEvent.Submitting -> state.copy(isSubmitting = event.submitting)
            }

        private fun load() {
            viewModelScope.launch {
                val (agreements, sanctions) =
                    coroutineScope {
                        val a = async { runCatching { accountRepository.getAgreements() }.getOrNull() }
                        val s = async { runCatching { accountRepository.getSanctions() }.getOrNull() }
                        a.await() to s.await()
                    }
                if (agreements == null && sanctions == null) {
                    dispatch(SettingsReducerEvent.LoadFinished)
                    return@launch
                }
                dispatch(
                    SettingsReducerEvent.Loaded(
                        reconsentCount = agreements?.reconsentRequired?.size ?: 0,
                        hasActiveSanction = sanctions?.activeSanction != null,
                    ),
                )
            }
        }

        /** 로그아웃은 서버 revoke 가 실패해도 로컬을 지운다 — 그 규칙은 UseCase 가 갖는다. */
        private fun logout() {
            if (currentState.isSubmitting) return
            dispatch(SettingsReducerEvent.Submitting(true))
            viewModelScope.launch {
                runCatching { logoutUseCase() }
                dispatch(SettingsReducerEvent.DialogDismissed)
                dispatch(SettingsReducerEvent.Submitting(false))
                goToLogin()
            }
        }

        /**
         * 탈퇴는 서버가 받아들였을 때만 로그인 화면으로 보낸다 — 실패했는데 내보내면 사용자는
         * 탈퇴된 줄 알고 앱을 지운다.
         */
        private fun withdraw() {
            if (currentState.isSubmitting) return
            dispatch(SettingsReducerEvent.Submitting(true))
            viewModelScope.launch {
                runCatching { withdrawUseCase() }
                    .onSuccess { result ->
                        dispatch(SettingsReducerEvent.DialogDismissed)
                        dispatch(SettingsReducerEvent.Submitting(false))
                        result.restoreNote?.let { emitEffect(SettingsEffect.ShowMessage(it)) }
                        goToLogin()
                    }.onFailure {
                        dispatch(SettingsReducerEvent.Submitting(false))
                        emitEffect(SettingsEffect.ShowMessage(it.message ?: "탈퇴하지 못했어요"))
                    }
            }
        }

        /** 백스택을 교체한다 — 뒤로가기로 로그인 전 화면에 돌아가면 토큰 없는 화면이 뜬다. */
        private fun goToLogin() {
            navigationHelper.replaceStackWith(NavRoute(AppRoutes.LOGIN))
        }
    }
