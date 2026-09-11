package com.ruleup.profile.presentation.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.notification.domain.navigation.NotificationCenterPage
import com.ruleup.notification.domain.navigation.NotificationSettingsPage
import com.ruleup.onboarding.domain.auth.usecase.LogoutUseCase
import com.ruleup.onboarding.domain.auth.usecase.WithdrawUseCase
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.domain.navigation.MyAgreementsPage
import com.ruleup.profile.domain.navigation.MySanctionsPage
import com.ruleup.profile.domain.navigation.MyWatchingPage
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import com.ruleup.report.domain.navigation.BlockListPage
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.domain.entity.InquirySummary
import com.ruleup.support.domain.navigation.InquiryCategoryPage
import com.ruleup.support.domain.navigation.InquiryListPage
import com.ruleup.support.domain.repository.InquiryRepository
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
        private val profileRepository: ProfileRepository,
        private val inquiryRepository: InquiryRepository,
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

                SettingsIntent.OpenNotificationSettings -> navigationHelper.navigateTo(NotificationSettingsPage)
                SettingsIntent.OpenNotificationCenter -> navigationHelper.navigateTo(NotificationCenterPage)

                SettingsIntent.OpenInquiry -> navigationHelper.navigateTo(InquiryCategoryPage)
                SettingsIntent.OpenInquiryHistory -> navigationHelper.navigateTo(InquiryListPage)

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
                        provider = event.provider,
                        reconsentCount = event.reconsentCount,
                        hasActiveSanction = event.hasActiveSanction,
                        answeredInquiryCount = event.answeredInquiryCount,
                    )

                SettingsReducerEvent.LoadFinished -> state.copy(isLoading = false)

                is SettingsReducerEvent.DialogShown -> state.copy(dialog = event.dialog)

                SettingsReducerEvent.DialogDismissed -> state.copy(dialog = null)

                is SettingsReducerEvent.Submitting -> state.copy(isSubmitting = event.submitting)
            }

        private fun load() {
            viewModelScope.launch {
                // 넷은 서로 독립이라 함께 던진다. 하나가 실패해도 나머지 행은 그대로 그린다 —
                // 제재 조회가 막혔다고 로그아웃까지 못 하게 만들 이유가 없다.
                val loaded =
                    coroutineScope {
                        val a = async { runCatching { accountRepository.getAgreements() }.getOrNull() }
                        val s = async { runCatching { accountRepository.getSanctions() }.getOrNull() }
                        val p = async { runCatching { profileRepository.getMyProfile() }.getOrNull() }
                        val i = async { runCatching { inquiryRepository.getInquiries() }.getOrNull() }
                        SettingsLoad(a.await(), s.await(), p.await(), i.await())
                    }
                if (loaded.isEmpty) {
                    dispatch(SettingsReducerEvent.LoadFinished)
                    return@launch
                }
                dispatch(
                    SettingsReducerEvent.Loaded(
                        provider = loaded.profile?.user?.provider,
                        reconsentCount = loaded.agreements?.reconsentRequired?.size ?: 0,
                        hasActiveSanction = loaded.sanctions?.activeSanction != null,
                        answeredInquiryCount =
                            loaded.inquiries.orEmpty().count { it.status == InquiryStatus.ANSWERED },
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

/**
 * 설정 허브가 한 번에 받아 오는 네 조각. 넷 다 실패했을 때만 뱃지 없이 화면을 띄우려고 묶었다 —
 * `Triple` 로는 네 번째가 들어가지 않고, 자리 순서로만 구분되면 호출부에서 뒤바뀌어도 드러나지 않는다.
 */
private data class SettingsLoad(
    val agreements: AgreementStatus?,
    val sanctions: SanctionHistory?,
    val profile: MyProfile?,
    val inquiries: List<InquirySummary>?,
) {
    val isEmpty: Boolean
        get() = agreements == null && sanctions == null && profile == null && inquiries == null
}
