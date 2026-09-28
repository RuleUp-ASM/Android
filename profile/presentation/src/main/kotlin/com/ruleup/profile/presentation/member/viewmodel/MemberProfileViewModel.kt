package com.ruleup.profile.presentation.member.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.user.FeatureCode
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import com.ruleup.profile.presentation.common.SuspendedBlock
import com.ruleup.profile.presentation.common.sanctionUntilLabel
import com.ruleup.report.domain.navigation.ReportPage
import com.ruleup.report.domain.repository.ReportRepository
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** 타인 프로필. */
@HiltViewModel
class MemberProfileViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val reportRepository: ReportRepository,
        private val accountRepository: AccountRepository,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
    ) : MviViewModel<MemberProfileIntent, MemberProfileState, MemberProfileReducerEvent, MemberProfileEffect>(
            MemberProfileState.initial,
        ) {
        /** 진입 인자로 받은 대상. */
        private var userId: String = ""

        override fun onIntent(intent: MemberProfileIntent) {
            when (intent) {
                is MemberProfileIntent.Load -> load(intent.userId, force = false)
                MemberProfileIntent.Retry -> load(userId, force = true)
                MemberProfileIntent.Back -> navigationHelper.navigateToBack()
                MemberProfileIntent.Report -> openReport()
                MemberProfileIntent.Unblock -> unblock()
                MemberProfileIntent.DismissReportBlock -> dispatch(MemberProfileReducerEvent.ReportBlocked(null))
                MemberProfileIntent.OpenSanctionHistory -> {
                    dispatch(MemberProfileReducerEvent.ReportBlocked(null))
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_SANCTIONS))
                }
            }
        }

        override fun reduce(
            state: MemberProfileState,
            event: MemberProfileReducerEvent,
        ): MemberProfileState =
            when (event) {
                MemberProfileReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null, isOffline = false)

                is MemberProfileReducerEvent.Loaded ->
                    state.copy(isLoading = false, profile = event.profile, errorMessage = null, isOffline = false)

                is MemberProfileReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message, isOffline = event.offline)

                is MemberProfileReducerEvent.Unblocking -> state.copy(isUnblocking = event.inProgress)

                is MemberProfileReducerEvent.ReportBlocked -> state.copy(reportBlock = event.block)
            }

        private fun load(
            userId: String,
            force: Boolean,
        ) {
            this.userId = userId
            // 대상을 모르면 조회할 것이 없다.
            if (userId.isBlank()) {
                dispatch(MemberProfileReducerEvent.Failed("누구의 프로필인지 알 수 없어요", offline = false))
                return
            }
            if (!force && currentState.profile != null) return
            dispatch(MemberProfileReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { profileRepository.getMemberProfile(userId) }
                    .onSuccess { dispatch(MemberProfileReducerEvent.Loaded(it)) }
                    .onFailure {
                        dispatch(
                            MemberProfileReducerEvent.Failed(
                                message = it.userFacingMessage("프로필을 불러오지 못했어요"),
                                offline = it is IOException,
                            ),
                        )
                    }
            }
        }

        /** 신고 화면으로 보낸다. */
        private fun openReport() {
            val profile = currentState.profile ?: return
            viewModelScope.launch {
                // 신고 기능만 정지된 계정도 여기서 막힌다
                val history = runCatching { accountRepository.getSanctions() }.getOrNull()
                if (history?.restriction?.blocks(FeatureCode.REPORT) == true) {
                    dispatch(
                        MemberProfileReducerEvent.ReportBlocked(
                            SuspendedBlock(until = history.activeSanction?.endsAt?.let(::sanctionUntilLabel)),
                        ),
                    )
                    return@launch
                }
                navigationHelper.navigateByRoute(
                    ReportPage(userId = profile.userId, targetName = profile.nickname).toRoute(),
                )
            }
        }

        private fun unblock() {
            val profile = currentState.profile ?: return
            if (currentState.isUnblocking) return
            dispatch(MemberProfileReducerEvent.Unblocking(true))
            viewModelScope.launch {
                runCatching { reportRepository.unblockUser(profile.userId) }
                    .onSuccess {
                        dispatch(MemberProfileReducerEvent.Unblocking(false))
                        // 해제하면 닉네임·사진이 원래 값으로 돌아온다
                        load(userId, force = true)
                    }.onFailure {
                        dispatch(MemberProfileReducerEvent.Unblocking(false))
                        messageHelper.showToast(it.userFacingMessage("차단을 해제하지 못했어요"))
                    }
            }
        }
    }
