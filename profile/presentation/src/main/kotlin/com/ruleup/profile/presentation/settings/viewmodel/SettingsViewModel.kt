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
import com.ruleup.support.domain.entity.InquirySummary
import com.ruleup.support.domain.navigation.InquiryCategoryPage
import com.ruleup.support.domain.navigation.InquiryListPage
import com.ruleup.support.domain.repository.InquiryReadStore
import com.ruleup.support.domain.repository.InquiryRepository
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 설정 허브 ViewModel. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val profileRepository: ProfileRepository,
        private val inquiryRepository: InquiryRepository,
        private val inquiryReadStore: InquiryReadStore,
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
                        newAnswerCount = event.newAnswerCount,
                    )

                SettingsReducerEvent.LoadFinished -> state.copy(isLoading = false)

                is SettingsReducerEvent.DialogShown -> state.copy(dialog = event.dialog)

                SettingsReducerEvent.DialogDismissed -> state.copy(dialog = null)

                is SettingsReducerEvent.Submitting -> state.copy(isSubmitting = event.submitting)
            }

        private fun load() {
            viewModelScope.launch {
                // 다섯은 서로 독립이라 함께 던진다.
                val (loaded, seenAnswers) =
                    coroutineScope {
                        val a = async { runCatching { accountRepository.getAgreements() }.getOrNull() }
                        val s = async { runCatching { accountRepository.getSanctions() }.getOrNull() }
                        val p = async { runCatching { profileRepository.getMyProfile() }.getOrNull() }
                        val i = async { runCatching { inquiryRepository.getInquiries() }.getOrNull() }
                        val seen = async { runCatching { inquiryReadStore.seenAnswers() }.getOrDefault(emptyMap()) }
                        SettingsLoad(a.await(), s.await(), p.await(), i.await()) to seen.await()
                    }
                if (loaded.isEmpty) {
                    dispatch(SettingsReducerEvent.LoadFinished)
                    return@launch
                }
                dispatch(
                    SettingsReducerEvent.Loaded(
                        provider =
                            loaded.profile
                                ?.user
                                ?.account
                                ?.provider,
                        reconsentCount = loaded.agreements?.reconsentRequired?.size ?: 0,
                        hasActiveSanction = loaded.sanctions?.activeSanction != null,
                        newAnswerCount =
                            loaded.inquiries.orEmpty().count { it.hasNewAnswer(seenAnswers[it.inquiryId]) },
                    ),
                )
            }
        }

        /** 로그아웃은 서버 revoke 가 실패해도 로컬을 지운다 */
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

        /** 탈퇴는 서버가 받아들였을 때만 로그인 화면으로 보낸다 */
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
                        emitEffect(SettingsEffect.ShowMessage(it.userFacingMessage("탈퇴하지 못했어요")))
                    }
            }
        }

        /** 백스택을 교체한다 */
        private fun goToLogin() {
            navigationHelper.replaceStackWith(NavRoute(AppRoutes.LOGIN))
        }
    }

/** 설정 허브가 한 번에 받아 오는 네 조각. */
private data class SettingsLoad(
    val agreements: AgreementStatus?,
    val sanctions: SanctionHistory?,
    val profile: MyProfile?,
    val inquiries: List<InquirySummary>?,
) {
    val isEmpty: Boolean
        get() = agreements == null && sanctions == null && profile == null && inquiries == null
}
