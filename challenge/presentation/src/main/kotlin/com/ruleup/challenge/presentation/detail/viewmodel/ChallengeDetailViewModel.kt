package com.ruleup.challenge.presentation.detail.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.analytics.domain.AnalyticsEvent
import com.ruleup.analytics.domain.AnalyticsLogger
import com.ruleup.challenge.domain.entity.WATCHER_FREE_LIMIT
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.challenge.domain.entity.WatcherLimitExceededException
import com.ruleup.challenge.domain.navigation.ChallengeTargetsPage
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.challenge.domain.usecase.CreateWatcherInvitationUseCase
import com.ruleup.challenge.domain.usecase.GetChallengeDetailUseCase
import com.ruleup.challenge.domain.usecase.GetChallengeSetupUseCase
import com.ruleup.challenge.domain.usecase.GetWatchersUseCase
import com.ruleup.challenge.domain.usecase.RemoveWatcherUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 챌린지 상세/참여 ViewModel.
 *
 * 상세 + 셋업 요구사항(GET setup)을 조회해, requiresAnchors/requiresTargetPackages 로 필요한 등록만
 * 유도한다: 권한 → (필요 시) 앱 등록 → (필요 시) 지도 앵커 → 시작.
 * 권한 확인/요청·모달 노출은 Context 가 필요해 화면(Composable)이 담당한다.
 */
@HiltViewModel
class ChallengeDetailViewModel
    @Inject
    constructor(
        private val getChallengeDetailUseCase: GetChallengeDetailUseCase,
        private val getChallengeSetupUseCase: GetChallengeSetupUseCase,
        private val getWatchersUseCase: GetWatchersUseCase,
        private val createWatcherInvitationUseCase: CreateWatcherInvitationUseCase,
        private val removeWatcherUseCase: RemoveWatcherUseCase,
        private val targetAppStore: TargetAppStore,
        private val analyticsLogger: AnalyticsLogger,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ChallengeDetailIntent, ChallengeDetailState, ChallengeDetailReducerEvent, ChallengeDetailEffect>(
            ChallengeDetailState.initial,
        ) {
        override fun onIntent(intent: ChallengeDetailIntent) {
            when (intent) {
                is ChallengeDetailIntent.Load -> load(intent.challengeId)
                ChallengeDetailIntent.RefreshSetup -> refreshSetup()
                ChallengeDetailIntent.RegisterApps -> registerApps()
                ChallengeDetailIntent.RegisterAnchor -> registerAnchor()
                ChallengeDetailIntent.PermissionGranted -> logSetupStep(AnalyticsEvent.SetupStepCompleted.STEP_PERMISSION)
                ChallengeDetailIntent.Proceed -> navigationHelper.navigateToBack()
                ChallengeDetailIntent.InviteWatcher -> inviteWatcher()
                is ChallengeDetailIntent.RemoveWatcher -> removeWatcher(intent.watcherId)
                ChallengeDetailIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ChallengeDetailState,
            event: ChallengeDetailReducerEvent,
        ): ChallengeDetailState =
            when (event) {
                is ChallengeDetailReducerEvent.Loading ->
                    state.copy(isLoading = true, challengeId = event.challengeId, errorMessage = null)

                is ChallengeDetailReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        detail = event.detail,
                        errorMessage = null,
                        setup = event.setup,
                        targetAppsRegistered = event.targetAppsRegistered,
                    )

                is ChallengeDetailReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is ChallengeDetailReducerEvent.SetupRefreshed ->
                    state.copy(setup = event.setup, targetAppsRegistered = event.targetAppsRegistered)

                is ChallengeDetailReducerEvent.WatchersLoaded -> state.copy(watchers = event.watchers)

                is ChallengeDetailReducerEvent.InvitingWatcher -> state.copy(isInvitingWatcher = event.inviting)
            }

        private fun load(challengeId: String) {
            if (currentState.detail?.challengeId == challengeId) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.Loading(challengeId))
                runCatching { getChallengeDetailUseCase(challengeId) }
                    .onSuccess { detail ->
                        // 셋업 요구사항은 실패해도(미구현/멤버 아님 등) 상세 렌더를 막지 않도록 흡수한다.
                        val setup = runCatching { getChallengeSetupUseCase(challengeId) }.getOrNull()
                        dispatch(
                            ChallengeDetailReducerEvent.Loaded(
                                detail = detail,
                                setup = setup,
                                targetAppsRegistered = targetAppStore.isRegistered(challengeId),
                            ),
                        )
                        // 감시자는 챌린지 × 참여자 단위 — 항상 조회를 시도하고, 성공하면(=참여자)
                        // 섹션을 노출한다. 미참여 403 등 실패는 흡수(섹션 숨김).
                        loadWatchers(challengeId)
                    }.onFailure { dispatch(ChallengeDetailReducerEvent.Failed(it.message ?: "챌린지를 불러오지 못했어요")) }
            }
        }

        // 등록 화면에서 돌아왔을 때 셋업 상태(앵커 바인딩 여부·앱 등록 여부)를 재확인해 버튼 모드를 갱신한다.
        private fun refreshSetup() {
            val id = currentState.detail?.challengeId ?: return
            viewModelScope.launch {
                val setup = runCatching { getChallengeSetupUseCase(id) }.getOrNull() ?: currentState.setup
                dispatch(
                    ChallengeDetailReducerEvent.SetupRefreshed(
                        setup = setup,
                        targetAppsRegistered = targetAppStore.isRegistered(id),
                    ),
                )
            }
        }

        private fun registerApps() {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            navigationHelper.navigateByRoute(ChallengeTargetsPage(id).toRoute())
        }

        // 셋업 퍼널 단계 완료 이벤트(권한/앱/앵커). 어느 단계에서 이탈하는지 관측한다.
        private fun logSetupStep(step: String) {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            analyticsLogger.log(AnalyticsEvent.SetupStepCompleted(step = step, challengeId = id))
        }

        private fun loadWatchers(challengeId: String) {
            viewModelScope.launch {
                runCatching { getWatchersUseCase(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.WatchersLoaded(it)) }
            }
        }

        /**
         * 내 감시자 초대 생성 → 본인 카카오톡 공유(스펙: 초대 전달은 사용자 본인 채널로만).
         * 무료 한도(참여자 기준 3명) 초과는 구독 안내 메시지로 분기한다.
         */
        private fun inviteWatcher() {
            val detail = currentState.detail ?: return
            if (currentState.isInvitingWatcher) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.InvitingWatcher(true))
                runCatching { createWatcherInvitationUseCase(detail.challengeId) }
                    .onSuccess { invitation ->
                        emitEffect(
                            ChallengeDetailEffect.ShareWatcherInvite(
                                card = invitation.inviteCard(challengeTitle = detail.title),
                                inviteUrl = invitation.inviteUrl,
                            ),
                        )
                        loadWatchers(detail.challengeId)
                    }.onFailure { throwable ->
                        val message =
                            if (throwable is WatcherLimitExceededException) {
                                "무료 감시자 ${WATCHER_FREE_LIMIT}명을 모두 사용했어요. 구독하면 무제한으로 추가할 수 있어요"
                            } else {
                                throwable.message ?: "감시자 초대에 실패했어요"
                            }
                        emitEffect(ChallengeDetailEffect.ShowMessage(message))
                    }
                dispatch(ChallengeDetailReducerEvent.InvitingWatcher(false))
            }
        }

        private fun removeWatcher(watcherId: String) {
            val challengeId = currentState.detail?.challengeId ?: return
            viewModelScope.launch {
                runCatching { removeWatcherUseCase(challengeId, watcherId) }
                    .onSuccess { loadWatchers(challengeId) }
                    .onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage(it.message ?: "감시자 해제에 실패했어요"))
                    }
            }
        }

        private fun registerAnchor() {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            // GPS 루틴 좌표 바인딩(verification/location) 으로 이동. 참여 API 가 memberId 를 돌려주지 않아
            // verification 의 기존 POC 관례대로 challengeMemberId 자리에 challengeId 를 넣는다.
            // 로컬 등록한 대상 앱을 함께 실어보내, 앵커 등록 화면이 setup 제출 시 앵커와 같이 전송하게 한다.
            navigationHelper.navigateByRoute(
                NavRoute(
                    AppRoutes.VERIFICATION_LOCATION,
                    mapOf(
                        "challengeMemberId" to id,
                        "defaultRadiusM" to "100.0",
                        "dwellMinutes" to "60",
                        "targetPackages" to targetAppStore.registered(id).joinToString(","),
                    ),
                ),
            )
        }
    }

/**
 * 카톡 공유 카드 문구. 초대자(나)의 닉네임이 들어간 스펙 메시지 ① 문구는 토큰 사용자를 아는
 * 서버 kakaoShare 페이로드가 담당하고, 없을 때만 닉네임 없는 일반 문구로 폴백한다.
 * (챌린지 생성자 닉네임을 쓰면 안 된다 — 초대자는 참여자 본인이다.)
 */
private fun WatcherInvitation.inviteCard(challengeTitle: String): WatcherInviteCard =
    kakaoShare
        ?: WatcherInviteCard(
            title = "당신을 루틴 감시자로 초대했어요",
            description = "[$challengeTitle]에서 약속을 지키는지 지켜봐 주세요. 실패하면 알림이 가요.",
            buttonLabel = "수락하기",
        )
