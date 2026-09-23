package com.ruleup.profile.presentation.member.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.user.FeatureCode
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.profile.domain.navigation.MemberProfilePage
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import com.ruleup.profile.presentation.common.SuspendedBlock
import com.ruleup.profile.presentation.common.sanctionUntilLabel
import com.ruleup.report.domain.navigation.ReportPage
import com.ruleup.report.domain.repository.ReportRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * 타인 프로필 (명세 GET /users/{userId}/profile · Figma `1466:2`·`1466:44`).
 *
 * 공개 범위가 좁은 게 이 화면의 전부다 — 닉네임·표시 티어·완주 개수만 보여 주고 나머지는
 * "공개되지 않아요" 한 줄로 닫는다. 없는 값을 0 으로 그리면 사용자가 그 사람의 통계를 봤다고 믿는다.
 *
 * 차단한 상대는 서버가 이미 임시 닉네임·기본 이미지로 마스킹해 내려준다. 화면은 거기에 해제
 * 경로만 더한다 — 왜 이름이 이상한지 설명하지 않으면 사용자는 상대가 개명했다고 읽는다.
 */
@HiltViewModel
class MemberProfileViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val reportRepository: ReportRepository,
        private val accountRepository: AccountRepository,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
        savedStateHandle: SavedStateHandle,
    ) : MviViewModel<MemberProfileIntent, MemberProfileState, MemberProfileReducerEvent, MemberProfileEffect>(
            MemberProfileState.initial,
        ) {
        private val userId: String = savedStateHandle[MemberProfilePage.ARG_USER_ID] ?: ""

        override fun onIntent(intent: MemberProfileIntent) {
            when (intent) {
                MemberProfileIntent.Load -> load(force = false)
                MemberProfileIntent.Retry -> load(force = true)
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

        private fun load(force: Boolean) {
            if (userId.isBlank()) return
            if (!force && currentState.profile != null) return
            dispatch(MemberProfileReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { profileRepository.getMemberProfile(userId) }
                    .onSuccess { dispatch(MemberProfileReducerEvent.Loaded(it)) }
                    .onFailure {
                        dispatch(
                            MemberProfileReducerEvent.Failed(
                                message = it.message ?: "프로필을 불러오지 못했어요",
                                offline = it is IOException,
                            ),
                        )
                    }
            }
        }

        /**
         * 신고 화면으로 보낸다. 대상 이름을 함께 넘겨 그 화면이 조회를 한 번 더 하지 않게 한다.
         *
         * 신고가 접수되면 상대가 내 화면에서 가려지므로, 돌아올 자리가 사라진다 — 그래서 신고 화면은
         * 이 화면 위에 쌓고 접수 후에는 스스로 닫는다.
         */
        private fun openReport() {
            val profile = currentState.profile ?: return
            viewModelScope.launch {
                // 신고 기능만 정지된 계정도 여기서 막힌다 — 전체 잠금이 아니라고 통과시키면
                // 정지된 기능이 서버 거절로만 드러난다.
                // 조회가 실패하면 보낸다 — 정지 여부를 모른다고 신고를 막으면 멀쩡한 사용자가 갇힌다.
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
                        // 해제하면 닉네임·사진이 원래 값으로 돌아온다 — 다시 받아야 화면이 사실과 맞는다.
                        load(force = true)
                    }.onFailure {
                        dispatch(MemberProfileReducerEvent.Unblocking(false))
                        messageHelper.showToast(it.message ?: "차단을 해제하지 못했어요")
                    }
            }
        }
    }
