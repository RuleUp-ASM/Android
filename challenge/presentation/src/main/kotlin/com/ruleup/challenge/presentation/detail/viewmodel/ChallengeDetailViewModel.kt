package com.ruleup.challenge.presentation.detail.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.DelegationAction
import com.ruleup.challenge.domain.entity.RoleAction
import com.ruleup.challenge.domain.entity.WATCHER_FREE_LIMIT
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.challenge.domain.entity.WatcherLimitExceededException
import com.ruleup.challenge.domain.navigation.ChallengeNoticeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeNoticesPage
import com.ruleup.challenge.domain.navigation.ChallengeRankingPage
import com.ruleup.challenge.domain.navigation.ChallengeTargetsPage
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.challenge.domain.usecase.ChangeMemberRoleUseCase
import com.ruleup.challenge.domain.usecase.CreateWatcherInvitationUseCase
import com.ruleup.challenge.domain.usecase.DeleteChallengeUseCase
import com.ruleup.challenge.domain.usecase.GetChallengeDetailUseCase
import com.ruleup.challenge.domain.usecase.GetChallengeMembersUseCase
import com.ruleup.challenge.domain.usecase.GetChallengeRoomUseCase
import com.ruleup.challenge.domain.usecase.GetChallengeSetupUseCase
import com.ruleup.challenge.domain.usecase.GetCurrentUserIdUseCase
import com.ruleup.challenge.domain.usecase.GetWatchersUseCase
import com.ruleup.challenge.domain.usecase.LeaveChallengeUseCase
import com.ruleup.challenge.domain.usecase.RemoveWatcherUseCase
import com.ruleup.challenge.domain.usecase.RequestDelegationUseCase
import com.ruleup.challenge.domain.usecase.RespondDelegationUseCase
import com.ruleup.challenge.presentation.observability.ChallengeDetailTtiPage
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.observability.domain.api.TtiTracker
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.model.TtiTimeline
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
        private val getChallengeRoomUseCase: GetChallengeRoomUseCase,
        private val getChallengeMembersUseCase: GetChallengeMembersUseCase,
        private val leaveChallengeUseCase: LeaveChallengeUseCase,
        private val deleteChallengeUseCase: DeleteChallengeUseCase,
        private val changeMemberRoleUseCase: ChangeMemberRoleUseCase,
        private val requestDelegationUseCase: RequestDelegationUseCase,
        private val respondDelegationUseCase: RespondDelegationUseCase,
        private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
        private val getWatchersUseCase: GetWatchersUseCase,
        private val createWatcherInvitationUseCase: CreateWatcherInvitationUseCase,
        private val removeWatcherUseCase: RemoveWatcherUseCase,
        private val targetAppStore: TargetAppStore,
        private val navigationHelper: NavigationHelper,
        private val ttiTracker: TtiTracker,
    ) : MviViewModel<ChallengeDetailIntent, ChallengeDetailState, ChallengeDetailReducerEvent, ChallengeDetailEffect>(
            ChallengeDetailState.initial,
        ) {
        override fun onIntent(intent: ChallengeDetailIntent) {
            when (intent) {
                is ChallengeDetailIntent.Load -> load(intent.challengeId)
                ChallengeDetailIntent.RefreshSetup -> refreshSetup()
                ChallengeDetailIntent.RegisterApps -> registerApps()
                ChallengeDetailIntent.RegisterAnchor -> registerAnchor()
                ChallengeDetailIntent.Proceed -> navigationHelper.navigateToBack()
                ChallengeDetailIntent.InviteWatcher -> inviteWatcher()
                is ChallengeDetailIntent.RemoveWatcher -> removeWatcher(intent.watcherId)
                ChallengeDetailIntent.OpenNotices -> openNotices()
                is ChallengeDetailIntent.OpenNotice -> openNotice(intent.noticeId)
                ChallengeDetailIntent.OpenRanking -> openRanking()
                ChallengeDetailIntent.OpenPendingReviews -> openPendingReviews()
                ChallengeDetailIntent.LeaveChallenge -> leaveChallenge()
                ChallengeDetailIntent.DeleteChallenge -> deleteChallenge()
                is ChallengeDetailIntent.PromoteMember -> changeRole(intent.userId, RoleAction.PROMOTE)
                is ChallengeDetailIntent.DemoteMember -> changeRole(intent.userId, RoleAction.DEMOTE)
                is ChallengeDetailIntent.RequestDelegation -> requestDelegation(intent.targetUserId)
                ChallengeDetailIntent.CancelDelegation -> cancelDelegation()
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

                is ChallengeDetailReducerEvent.RoomLoaded -> state.copy(room = event.room)

                is ChallengeDetailReducerEvent.MembersLoaded -> state.copy(members = event.members)

                is ChallengeDetailReducerEvent.MemberActionLoading -> state.copy(isMemberActionLoading = event.loading)

                is ChallengeDetailReducerEvent.DelegationRequested ->
                    state.copy(pendingDelegation = event.ticket, pendingDelegationNickname = event.targetNickname)

                ChallengeDetailReducerEvent.DelegationCleared ->
                    state.copy(pendingDelegation = null, pendingDelegationNickname = null)

                is ChallengeDetailReducerEvent.MyUserIdLoaded -> state.copy(myUserId = event.userId)
            }

        private fun load(challengeId: String) {
            if (currentState.detail?.challengeId == challengeId) return
            // 현재 사용자 ID 는 멤버 목록의 "내 행" 식별용 — 실패해도 흡수(본인 한정 액션만 숨겨진다).
            if (currentState.myUserId == null) {
                viewModelScope.launch {
                    val userId = runCatching { getCurrentUserIdUseCase() }.getOrNull()
                    dispatch(ChallengeDetailReducerEvent.MyUserIdLoaded(userId))
                }
            }
            viewModelScope.launch {
                // 화면 진입 → 사용 가능 상태까지의 TTI. 네비게이션 시 이전 세션은 ScreenTracker 가 정리한다.
                ttiTracker.start(ChallengeDetailTtiPage, ScreenKey(AppRoutes.CHALLENGE_DETAIL))
                dispatch(ChallengeDetailReducerEvent.Loading(challengeId))
                ttiTracker.beginPhase(ChallengeDetailTtiPage, TtiTimeline.API_RESPONSE)
                runCatching { getChallengeDetailUseCase(challengeId) }
                    .onSuccess { detail ->
                        ttiTracker.endPhase(ChallengeDetailTtiPage, TtiTimeline.API_RESPONSE)
                        ttiTracker.beginPhase(ChallengeDetailTtiPage, TtiTimeline.VIEW_BINDING)
                        // 셋업 요구사항은 실패해도(미구현/멤버 아님 등) 상세 렌더를 막지 않도록 흡수한다.
                        val setup = runCatching { getChallengeSetupUseCase(challengeId) }.getOrNull()
                        dispatch(
                            ChallengeDetailReducerEvent.Loaded(
                                detail = detail,
                                setup = setup,
                                targetAppsRegistered = targetAppStore.isRegistered(challengeId),
                            ),
                        )
                        // 상세가 화면 상태로 반영된 시점 = 사용 가능. 감시자·방 홈은 부가 섹션이라 기다리지 않는다.
                        ttiTracker.endPhase(ChallengeDetailTtiPage, TtiTimeline.VIEW_BINDING)
                        ttiTracker.complete(ChallengeDetailTtiPage)
                        // 감시자는 챌린지 × 참여자 단위 — 항상 조회를 시도하고, 성공하면(=참여자)
                        // 섹션을 노출한다. 미참여 403 등 실패는 흡수(섹션 숨김).
                        loadWatchers(challengeId)
                        // 방 홈은 그룹 챌린지의 ACTIVE 멤버만 — 조회 성공 시 방 홈으로 확장 렌더링.
                        if (detail.mode == ChallengeMode.GROUP) loadRoom(challengeId)
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
            // 공지 상세를 읽고 돌아오면 미읽음 수가 바뀌므로 방 홈도 함께 재조회한다.
            if (currentState.room != null) loadRoom(id)
        }

        // 비멤버/솔로의 403 등 실패는 흡수 — room 이 null 이면 기존 공개 상세 그대로 렌더링된다.
        private fun loadRoom(challengeId: String) {
            viewModelScope.launch {
                runCatching { getChallengeRoomUseCase(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.RoomLoaded(it)) }
            }
            loadMembers(challengeId)
        }

        // 멤버 목록은 방 홈 부가 정보 — 실패해도(권한 등) 방 홈 렌더를 막지 않도록 흡수한다.
        private fun loadMembers(challengeId: String) {
            viewModelScope.launch {
                runCatching { getChallengeMembersUseCase(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.MembersLoaded(it)) }
            }
        }

        /** 탈퇴(본인). 성공 시 안내 후 이전 화면으로. OWNER 등 실패 사유는 서버 메시지로 노출. */
        private fun leaveChallenge() {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isMemberActionLoading) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(true))
                runCatching { leaveChallengeUseCase(id) }
                    .onSuccess { result ->
                        emitEffect(
                            ChallengeDetailEffect.ShowMessage(
                                if (result.penaltyApplied) "탈퇴했어요. 진행 이력이 있어 탈퇴 패널티가 적용됐어요" else "챌린지에서 나갔어요",
                            ),
                        )
                        navigationHelper.navigateToBack()
                    }.onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage(it.message ?: "탈퇴에 실패했어요"))
                    }
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(false))
            }
        }

        /** 삭제(방장). 참여자 0명일 때만 가능 — 실패 사유는 서버 메시지로 노출. */
        private fun deleteChallenge() {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isMemberActionLoading) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(true))
                runCatching { deleteChallengeUseCase(id) }
                    .onSuccess { result ->
                        emitEffect(
                            ChallengeDetailEffect.ShowMessage(
                                if (result.penaltyApplied) "챌린지를 삭제했어요. 진행 이력이 있어 패널티가 적용됐어요" else "챌린지를 삭제했어요",
                            ),
                        )
                        navigationHelper.navigateToBack()
                    }.onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage(it.message ?: "삭제에 실패했어요"))
                    }
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(false))
            }
        }

        /** 공동 관리자 임명/해제(방장). 성공 시 멤버 목록을 재조회한다. */
        private fun changeRole(
            userId: String,
            action: RoleAction,
        ) {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isMemberActionLoading) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(true))
                runCatching { changeMemberRoleUseCase(id, userId, action) }
                    .onSuccess {
                        val message = if (action == RoleAction.PROMOTE) "공동 관리자로 임명했어요" else "공동 관리자를 해제했어요"
                        emitEffect(ChallengeDetailEffect.ShowMessage(message))
                        loadMembers(id)
                    }.onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage(it.message ?: "권한 변경에 실패했어요"))
                    }
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(false))
            }
        }

        /** 방장 위임 요청(방장 → 공동 관리자). 생성된 티켓을 배너로 노출한다. */
        private fun requestDelegation(targetUserId: String) {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isMemberActionLoading) return
            val nickname =
                currentState.members
                    ?.members
                    ?.firstOrNull { it.userId == targetUserId }
                    ?.nickname
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(true))
                runCatching { requestDelegationUseCase(id, targetUserId) }
                    .onSuccess { ticket ->
                        dispatch(ChallengeDetailReducerEvent.DelegationRequested(ticket, nickname))
                        emitEffect(ChallengeDetailEffect.ShowMessage("방장 위임을 요청했어요. 상대가 수락하면 방장이 넘어가요"))
                    }.onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage(it.message ?: "방장 위임 요청에 실패했어요"))
                    }
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(false))
            }
        }

        /** 대기 중인 방장 위임 요청 취소(요청 OWNER). */
        private fun cancelDelegation() {
            val id = currentState.detail?.challengeId ?: return
            val delegationId = currentState.pendingDelegation?.delegationId ?: return
            if (currentState.isMemberActionLoading) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(true))
                runCatching { respondDelegationUseCase(id, delegationId, DelegationAction.CANCEL) }
                    .onSuccess {
                        dispatch(ChallengeDetailReducerEvent.DelegationCleared)
                        emitEffect(ChallengeDetailEffect.ShowMessage("방장 위임 요청을 취소했어요"))
                    }.onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage(it.message ?: "위임 요청 취소에 실패했어요"))
                    }
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(false))
            }
        }

        private fun openNotices() {
            val room = currentState.room ?: return
            val id = currentState.detail?.challengeId ?: return
            navigationHelper.navigateByRoute(
                ChallengeNoticesPage(
                    challengeId = id,
                    canManage = room.myRole.canManage,
                ).toRoute(),
            )
        }

        private fun openNotice(noticeId: String) {
            val room = currentState.room ?: return
            val id = currentState.detail?.challengeId ?: return
            navigationHelper.navigateByRoute(
                ChallengeNoticeDetailPage(
                    challengeId = id,
                    noticeId = noticeId,
                    canManage = room.myRole.canManage,
                ).toRoute(),
            )
        }

        private fun openRanking() {
            val id = currentState.detail?.challengeId ?: return
            navigationHelper.navigateByRoute(ChallengeRankingPage(challengeId = id).toRoute())
        }

        // 확인 대기함(verification)으로 이동. feature 간 직접 의존 없이 AppRoutes 경로로 라우팅한다.
        private fun openPendingReviews() {
            val id = currentState.detail?.challengeId ?: return
            navigationHelper.navigateByRoute(
                NavRoute(AppRoutes.VERIFICATION_PENDING_REVIEWS, mapOf("challengeId" to id)),
            )
        }

        private fun registerApps() {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            navigationHelper.navigateByRoute(ChallengeTargetsPage(id).toRoute())
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
            // GPS 루틴 좌표 바인딩(verification/location) 으로 이동. 멤버 키(지오펜스 requestId)는
            // verification 이 세션 userId 와 challengeId 로 파생한다.
            // 로컬 등록한 대상 앱을 함께 실어보내, 앵커 등록 화면이 setup 제출 시 앵커와 같이 전송하게 한다.
            navigationHelper.navigateByRoute(
                NavRoute(
                    AppRoutes.VERIFICATION_LOCATION,
                    mapOf(
                        "challengeId" to id,
                        "defaultRadiusM" to "500.0",
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
