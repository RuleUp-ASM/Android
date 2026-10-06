package com.ruleup.challenge.presentation.detail.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ChallengeNotCloneableException
import com.ruleup.challenge.domain.entity.ChallengeNotFoundException
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.challenge.domain.entity.RankingMode
import com.ruleup.challenge.domain.entity.ThreadCursorInvalidException
import com.ruleup.challenge.domain.entity.ThreadPolicy
import com.ruleup.challenge.domain.entity.WATCHER_FREE_LIMIT
import com.ruleup.challenge.domain.entity.WatcherLimitExceededException
import com.ruleup.challenge.domain.logging.ChallengeEvents
import com.ruleup.challenge.domain.logging.RankingViewScope
import com.ruleup.challenge.domain.navigation.ChallengeConfirmPage
import com.ruleup.challenge.domain.navigation.ChallengeRankingPage
import com.ruleup.challenge.domain.navigation.ChallengeSettingsPage
import com.ruleup.challenge.domain.navigation.ChallengeTargetsPage
import com.ruleup.challenge.domain.navigation.ChallengeWatchersPage
import com.ruleup.challenge.domain.navigation.MyChallengesPage
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.challenge.domain.repository.RoomRepository
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.challenge.domain.repository.WatcherRepository
import com.ruleup.challenge.presentation.watcher.inviteCard
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.token.TokenRepository
import com.ruleup.logging.domain.BizLogger
import com.ruleup.notification.domain.repository.NotificationRepository
import com.ruleup.profile.domain.navigation.MemberProfilePage
import com.ruleup.report.domain.entity.ReportContext
import com.ruleup.report.domain.entity.ReportException
import com.ruleup.report.domain.entity.ReportFailure
import com.ruleup.report.domain.entity.ReportTarget
import com.ruleup.report.domain.repository.ReportRepository
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.entity.AppealNotFailedException
import com.ruleup.verification.domain.entity.AppealWindowClosedException
import com.ruleup.verification.domain.entity.InvalidAppealReasonException
import com.ruleup.verification.domain.navigation.VerificationManualPage
import com.ruleup.verification.domain.navigation.VerificationPermissionRepairPage
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import com.ruleup.verification.domain.repository.VerificationRepository
import com.ruleup.verification.domain.usecase.AgreeVerificationConsentUseCase
import com.ruleup.verification.domain.usecase.CheckVerificationAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.YearMonth
import javax.inject.Inject

/** 챌린지 상세/참여 ViewModel. */
@HiltViewModel
class ChallengeDetailViewModel
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
        private val roomRepository: RoomRepository,
        private val watcherRepository: WatcherRepository,
        private val verificationRepository: VerificationRepository,
        private val permissionStatusProvider: PermissionStatusProvider,
        private val exploreRepository: ExploreRepository,
        private val tokenRepository: TokenRepository,
        private val bizLogger: BizLogger,
        private val targetAppStore: TargetAppStore,
        private val reportRepository: ReportRepository,
        private val notificationRepository: NotificationRepository,
        private val navigationHelper: NavigationHelper,
        private val checkVerificationAccess: CheckVerificationAccessUseCase,
        private val agreeVerificationConsent: AgreeVerificationConsentUseCase,
        private val restrictionProvider: com.ruleup.onboarding.domain.account.AccountRestrictionProvider =
            com.ruleup.onboarding.domain.account
                .AccountRestrictionProvider {
                    com.ruleup.domain.entity.user.AccountRestriction.None
                },
        private val pendingDraft: com.ruleup.challenge.presentation.create.PendingChallengeDraft =
            com.ruleup.challenge.presentation.create
                .PendingChallengeDraft(),
    ) : MviViewModel<ChallengeDetailIntent, ChallengeDetailState, ChallengeDetailReducerEvent, ChallengeDetailEffect>(
            ChallengeDetailState.initial,
        ) {
        override fun onIntent(intent: ChallengeDetailIntent) {
            when (intent) {
                is ChallengeDetailIntent.Load -> load(intent.challengeId)
                ChallengeDetailIntent.RefreshSetup -> refreshSetup()
                ChallengeDetailIntent.RegisterApps -> registerApps()
                ChallengeDetailIntent.RegisterAnchor -> registerAnchor()
                ChallengeDetailIntent.Proceed -> join()
                ChallengeDetailIntent.OpenVerificationAccess -> {
                    if (!currentState.isAccessSubmitting) {
                        joinAfterAccess = false
                        checkAccessThenJoin()
                    }
                }
                ChallengeDetailIntent.ConfirmVerificationAccess ->
                    if (currentState.pendingAccess != null) checkAccessThenJoin(agreeAndRequest = true)
                ChallengeDetailIntent.VerificationPermissionsReturned ->
                    if (currentState.pendingAccess != null) checkAccessThenJoin()
                ChallengeDetailIntent.DismissVerificationAccess ->
                    if (!currentState.isAccessSubmitting) dispatch(ChallengeDetailReducerEvent.VerificationAccessRequested(null))
                ChallengeDetailIntent.CloneChallenge -> clone()

                ChallengeDetailIntent.OpenReport -> openReport()
                is ChallengeDetailIntent.OpenMemberProfile ->
                    navigationHelper.navigateTo(MemberProfilePage(intent.userId))
                is ChallengeDetailIntent.OpenUserReport -> openReport(intent.userId)
                is ChallengeDetailIntent.SelectReportReason ->
                    dispatch(ChallengeDetailReducerEvent.ReportReasonSelected(intent.reason))
                ChallengeDetailIntent.SubmitReport -> submitReport()
                ChallengeDetailIntent.DismissReport -> {
                    // 챌린지를 신고했으면 그 화면에 머물 이유가 없어 이전 화면으로 돌아간다. 사용자 신고는 방에 남는다.
                    val leave = currentState.reportResult != null && currentState.reportUserId == null
                    dispatch(ChallengeDetailReducerEvent.ReportSheetDismissed)
                    if (leave) navigationHelper.navigateToBack()
                }

                ChallengeDetailIntent.OpenSettings ->
                    currentState.detail?.challengeId?.let {
                        navigationHelper.navigateByRoute(ChallengeSettingsPage(it).toRoute())
                    }
                ChallengeDetailIntent.DismissJoinBlock -> dispatch(ChallengeDetailReducerEvent.JoinBlockDismissed)
                ChallengeDetailIntent.RetryJoin -> {
                    dispatch(ChallengeDetailReducerEvent.JoinRetryable(false))
                    join()
                }
                ChallengeDetailIntent.DismissJoinRetry -> dispatch(ChallengeDetailReducerEvent.JoinRetryable(false))
                ChallengeDetailIntent.FollowJoinBlockAction -> followJoinBlockAction()
                ChallengeDetailIntent.InviteWatcher -> inviteWatcher()
                ChallengeDetailIntent.OpenWatchers -> openWatchers()
                ChallengeDetailIntent.InviteMember -> inviteMember()
                is ChallengeDetailIntent.SelectTab -> selectTab(intent.tab)

                is ChallengeDetailIntent.ShiftCalendarMonth -> shiftCalendarMonth(intent.offset)

                is ChallengeDetailIntent.ToggleMute -> toggleMute(intent.muted)
                ChallengeDetailIntent.LoadMoreThreads -> loadThreads(next = true)
                ChallengeDetailIntent.RetryThreads -> loadThreads(next = true, retry = true)
                is ChallengeDetailIntent.SelectRankingScope -> selectRankingScope(intent.scope)
                ChallengeDetailIntent.LoadMoreCrossRanking -> loadCrossRanking(next = true)
                ChallengeDetailIntent.OpenRanking -> openRanking()
                is ChallengeDetailIntent.PickAppealImage -> uploadAppealImage(intent.imageUri)
                ChallengeDetailIntent.OpenPermissionRepair ->
                    navigationHelper.navigateByRoute(
                        VerificationPermissionRepairPage.forPermissions(
                            currentState.detail
                                ?.verification
                                ?.requiredPermissions
                                .orEmpty(),
                        ),
                    )
                ChallengeDetailIntent.OpenManualCheck -> openManualCheck()
                ChallengeDetailIntent.RefreshPermissions -> refreshPermissions()
                ChallengeDetailIntent.DismissAppeal -> dispatch(ChallengeDetailReducerEvent.AppealReset)
                ChallengeDetailIntent.AcknowledgeResult -> acknowledgeResult()
                is ChallengeDetailIntent.SubmitAppeal -> submitAppeal(intent.verificationId, intent.reason)
                ChallengeDetailIntent.LeaveChallenge -> leaveChallenge()
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

                is ChallengeDetailReducerEvent.MyUserIdLoaded -> state.copy(myUserId = event.userId)

                is ChallengeDetailReducerEvent.Joining -> state.copy(isJoining = event.joining)

                is ChallengeDetailReducerEvent.JoinBlocked ->
                    state.copy(isJoining = false, joinBlock = event.block)

                is ChallengeDetailReducerEvent.JoinRetryable -> state.copy(joinRetryable = event.visible)

                ChallengeDetailReducerEvent.JoinBlockDismissed -> state.copy(joinBlock = null)

                is ChallengeDetailReducerEvent.Cloning -> state.copy(isCloning = event.cloning)

                is ChallengeDetailReducerEvent.TabSelected -> state.copy(selectedTab = event.tab)

                is ChallengeDetailReducerEvent.RankingScopeSelected -> state.copy(rankingScope = event.scope)

                is ChallengeDetailReducerEvent.ThreadsLoading ->
                    state.copy(
                        isThreadsLoading = event.first,
                        isThreadsPaging = !event.first,
                        threadsError = null,
                    )

                is ChallengeDetailReducerEvent.ThreadsLoaded ->
                    state.copy(
                        isThreadsLoading = false,
                        isThreadsPaging = false,
                        threadsError = null,
                        threads = if (event.reset) event.page.items else state.threads + event.page.items,
                        threadsCursor = event.page.nextCursor,
                    )

                is ChallengeDetailReducerEvent.ThreadsFailed ->
                    state.copy(
                        isThreadsLoading = false,
                        isThreadsPaging = false,
                        threadsError = event.message,
                    )

                is ChallengeDetailReducerEvent.MuteLoaded ->
                    state.copy(isMuted = event.muted, isMuteSubmitting = false)

                is ChallengeDetailReducerEvent.MuteSubmitting -> state.copy(isMuteSubmitting = event.submitting)

                is ChallengeDetailReducerEvent.CalendarMonthChanged ->
                    // 이전 달 색이 남으면 잘못된 기록으로 읽힌다
                    state.copy(calendarMonth = event.month, calendar = null)

                is ChallengeDetailReducerEvent.CalendarLoading -> state.copy(isCalendarLoading = event.loading)

                is ChallengeDetailReducerEvent.CalendarLoaded ->
                    state.copy(isCalendarLoading = false, calendar = event.calendar)

                is ChallengeDetailReducerEvent.RankingLoading -> state.copy(isRankingLoading = event.loading)

                is ChallengeDetailReducerEvent.RankingLoaded ->
                    state.copy(isRankingLoading = false, ranking = event.ranking)

                is ChallengeDetailReducerEvent.CrossRankingLoading -> state.copy(isCrossRankingLoading = event.loading)

                is ChallengeDetailReducerEvent.TodayResultLoaded -> state.copy(todayResult = event.result)
                ChallengeDetailReducerEvent.ResultAcknowledged -> state.copy(resultAcknowledged = true)
                is ChallengeDetailReducerEvent.AppealImageUploading -> state.copy(isUploadingAppealImage = event.uploading)
                is ChallengeDetailReducerEvent.AppealImageUploaded -> state.copy(appealImageUrl = event.imageUrl)
                is ChallengeDetailReducerEvent.AppealReasonRejected -> state.copy(appealReasonError = event.message)
                is ChallengeDetailReducerEvent.PermissionsCaptured -> state.copy(permissions = event.permissions)
                ChallengeDetailReducerEvent.AppealReset ->
                    state.copy(appealImageUrl = null, isUploadingAppealImage = false, appealReasonError = null)

                is ChallengeDetailReducerEvent.SubmittingAppeal -> state.copy(isSubmittingAppeal = event.submitting)

                ChallengeDetailReducerEvent.ReportSheetOpened -> state.copy(isReportSheetOpen = true, reportUserId = null)

                is ChallengeDetailReducerEvent.UserReportSheetOpened ->
                    state.copy(isReportSheetOpen = true, reportUserId = event.userId)

                is ChallengeDetailReducerEvent.VerificationAccessRequested -> state.copy(pendingAccess = event.access)
                is ChallengeDetailReducerEvent.VerificationAccessSubmitting -> state.copy(isAccessSubmitting = event.submitting)

                ChallengeDetailReducerEvent.ReportSheetDismissed ->
                    // 다음에 열 때 지난 선택이 남지 않게 비운다.
                    state.copy(
                        isReportSheetOpen = false,
                        reportUserId = null,
                        selectedReportReason = null,
                        isSubmittingReport = false,
                        reportResult = null,
                    )

                is ChallengeDetailReducerEvent.ReportReasonSelected ->
                    state.copy(selectedReportReason = event.reason)

                is ChallengeDetailReducerEvent.SubmittingReport ->
                    state.copy(isSubmittingReport = event.submitting)

                is ChallengeDetailReducerEvent.ReportAccepted ->
                    state.copy(isSubmittingReport = false, reportResult = event.result)

                is ChallengeDetailReducerEvent.CrossRankingLoaded ->
                    state.copy(
                        isCrossRankingLoading = false,
                        crossRanking =
                            if (event.append && state.crossRanking != null) {
                                event.ranking.copy(items = state.crossRanking.items + event.ranking.items)
                            } else {
                                event.ranking
                            },
                    )
            }

        // 참여 재시도에서도 철회된 권한·동의를 다시 확인하도록 통과 표시는 한 번만 쓴다.
        private var joinAccessChecked = false
        private var joinAfterAccess = false

        private fun checkAccessThenJoin(agreeAndRequest: Boolean = false) {
            if (currentState.isAccessSubmitting) return
            val detail = currentState.detail ?: return
            val requiredPermissions = currentState.setup?.requiredPermissions ?: detail.verification.requiredPermissions
            val consents = currentState.pendingAccess?.missingConsents.orEmpty()
            dispatch(ChallengeDetailReducerEvent.VerificationAccessSubmitting(true))
            viewModelScope.launch {
                runCatching {
                    if (agreeAndRequest) agreeVerificationConsent(consents)
                    checkVerificationAccess(requiredPermissions)
                }.onSuccess { access ->
                    dispatch(ChallengeDetailReducerEvent.PermissionsCaptured(access.permissions))
                    dispatch(ChallengeDetailReducerEvent.VerificationAccessSubmitting(false))
                    if (access.missingConsents.isEmpty() && access.missingPermissions.isEmpty()) {
                        dispatch(ChallengeDetailReducerEvent.VerificationAccessRequested(null))
                        if (joinAfterAccess) {
                            joinAccessChecked = true
                            join()
                        }
                    } else {
                        dispatch(ChallengeDetailReducerEvent.VerificationAccessRequested(access))
                        if (agreeAndRequest && access.missingConsents.isEmpty()) {
                            emitEffect(ChallengeDetailEffect.RequestPermissions(access.missingPermissions))
                        }
                    }
                }.onFailure {
                    dispatch(ChallengeDetailReducerEvent.VerificationAccessSubmitting(false))
                    emitEffect(ChallengeDetailEffect.ShowMessage("권한과 동의 상태를 확인하지 못했어요. 잠시 후 다시 시도해 주세요"))
                }
            }
        }

        /** 가입. */
        private fun join() {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isJoining || currentState.isAccessSubmitting) return
            val detail = currentState.detail
            if (!joinAccessChecked) {
                joinAfterAccess = true
                checkAccessThenJoin()
                return
            }
            joinAccessChecked = false
            bizLogger.record(
                ChallengeEvents.challengeJoinAttempt(
                    challengeId = id,
                    eligible = detail?.gate?.eligible ?: false,
                    isFull = detail?.isFull ?: false,
                ),
            )
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.Joining(true))
                runCatching { challengeRepository.join(id) }
                    .onSuccess { result ->
                        dispatch(ChallengeDetailReducerEvent.Joining(false))
                        // 탐색→참여 전환율의 분자.
                        bizLogger.record(ChallengeEvents.challengeJoinResult(challengeId = id, success = true))
                        // 사이클 중간 입장이면 언제부터 판정되는지 알려준다(사이클은 1주 고정).
                        result.countFromCycle?.let {
                            emitEffect(ChallengeDetailEffect.ShowMessage("${cycleStartLabel(it)}부터 인증이 집계돼요"))
                        }
                        load(id, force = true)
                    }.onFailure { error ->
                        bizLogger.record(
                            ChallengeEvents.challengeJoinResult(
                                challengeId = id,
                                success = false,
                                // 게이트 차단 분포를 보려면 reason 이 곧 에러 코드다.
                                errorCode = (error as? JoinBlockedException)?.reason?.value ?: "UNKNOWN",
                            ),
                        )
                        when (error) {
                            is JoinBlockedException -> {
                                // ALREADY_JOINED 는 알릴 게 없다
                                if (error.reason?.isAlreadyJoined == true) {
                                    dispatch(ChallengeDetailReducerEvent.Joining(false))
                                    load(id, force = true)
                                } else {
                                    dispatch(
                                        ChallengeDetailReducerEvent.JoinBlocked(
                                            JoinBlock(reason = error.reason, rejoinAvailableAt = error.rejoinAvailableAt),
                                        ),
                                    )
                                    // 정원은 수시로 변한다
                                    if (error.reason?.needsRefresh == true) load(id, force = true)
                                }
                            }

                            is ChallengeNotFoundException -> {
                                dispatch(ChallengeDetailReducerEvent.Joining(false))
                                emitEffect(ChallengeDetailEffect.ShowMessage(error.message.orEmpty()))
                                navigationHelper.navigateToBack()
                            }

                            // 연결 문제는 다시 눌러 보면 되는 실패다
                            is IOException -> {
                                dispatch(ChallengeDetailReducerEvent.Joining(false))
                                dispatch(ChallengeDetailReducerEvent.JoinRetryable(true))
                            }

                            else -> {
                                dispatch(ChallengeDetailReducerEvent.Joining(false))
                                emitEffect(ChallengeDetailEffect.ShowMessage(error.userFacingMessage("참여하지 못했어요")))
                            }
                        }
                    }
            }
        }

        /** 복제 → 생성 확인 화면. */
        private fun clone() {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isCloning) return
            bizLogger.record(ChallengeEvents.challengeCloneClick(id))
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.Cloning(true))
                runCatching { exploreRepository.clone(id) }
                    .onSuccess { draft ->
                        pendingDraft.put(draft)
                        dispatch(ChallengeDetailReducerEvent.Cloning(false))
                        navigationHelper.navigateTo(ChallengeConfirmPage)
                    }.onFailure { error ->
                        dispatch(ChallengeDetailReducerEvent.Cloning(false))
                        val message =
                            when (error) {
                                is ChallengeNotCloneableException -> error.message
                                is ChallengeNotFoundException -> error.message
                                else -> error.userFacingMessage("복제하지 못했어요")
                            }
                        emitEffect(ChallengeDetailEffect.ShowMessage(message.orEmpty()))
                    }
            }
        }

        /** 차단 시트의 CTA. */
        private fun followJoinBlockAction() {
            val reason = currentState.joinBlock?.reason
            dispatch(ChallengeDetailReducerEvent.JoinBlockDismissed)
            when (reason) {
                JoinBlockReason.FREE_LIMIT -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                JoinBlockReason.TIER_GATE -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_HOME))
                JoinBlockReason.FULL -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_EXPLORE))
                JoinBlockReason.CHALLENGE_COMPLETED -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_EXPLORE))
                else -> Unit
            }
        }

        // 상세 진입은 전환 분모라 화면당 1회다.
        private var detailViewLogged = false

        // 방 진입도 같은 이유로 화면당 1회다.
        private var roomViewLogged = false

        // 빈 피드는 같은 화면에서 여러 번 그려질 수 있어 노출 로그는 한 번만 남긴다.
        private var emptyFeedLogged = false

        private fun load(
            challengeId: String,
            force: Boolean = false,
        ) {
            // 정원·자격은 수시로 변한다
            if (!force && currentState.detail?.challengeId == challengeId) return
            // 현재 사용자 ID 는 멤버 목록의 "내 행" 식별용
            if (currentState.myUserId == null) {
                viewModelScope.launch {
                    val userId = runCatching { tokenRepository.getUserId() }.getOrNull()
                    dispatch(ChallengeDetailReducerEvent.MyUserIdLoaded(userId))
                }
            }
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.Loading(challengeId))
                runCatching { challengeRepository.getChallenge(challengeId) }
                    .onSuccess { detail ->
                        // 셋업 요구사항은 실패해도(미구현/멤버 아님 등) 상세 렌더를 막지 않도록 흡수한다.
                        val setup = runCatching { challengeRepository.getSetupInfo(challengeId) }.getOrNull()
                        // 그룹 멤버는 방을 먼저 받아 둔다 — 상세만 먼저 그리면 참여 전 화면이 잠깐 떴다가 방 화면으로 바뀐다.
                        val prefetchedRoom =
                            if (detail.mode.isGroup && detail.myRole.isMember) {
                                runCatching { roomRepository.getRoom(challengeId) }.getOrNull()
                            } else {
                                null
                            }
                        dispatch(
                            ChallengeDetailReducerEvent.Loaded(
                                detail = detail,
                                setup = setup,
                                targetAppsRegistered = targetAppStore.isRegistered(challengeId),
                            ),
                        )
                        // 상세→참여 전환의 분모.
                        if (!detailViewLogged) {
                            detailViewLogged = true
                            bizLogger.record(
                                ChallengeEvents.challengeDetailView(
                                    challengeId = detail.challengeId,
                                    // 카드에서 넘어온 경로는 아직 라우트 인자로 전달되지 않는다(오픈 이슈).
                                    source = null,
                                    eligible = detail.gate.eligible,
                                    isFull = detail.isFull,
                                ),
                            )
                        }
                        // 감시자는 챌린지 × 참여자 단위
                        loadWatchers(challengeId)
                        // 오늘 인증은 솔로도 필요하다
                        loadTodayResult(challengeId)
                        // 방 홈은 그룹 챌린지의 ACTIVE 멤버만
                        if (detail.mode.isGroup) loadRoom(challengeId, prefetchedRoom) else loadCalendar(challengeId)
                    }.onFailure { dispatch(ChallengeDetailReducerEvent.Failed(it.userFacingMessage("챌린지를 불러오지 못했어요"))) }
            }
        }

        // 등록 화면에서 돌아왔을 때 셋업 상태(앵커 바인딩 여부·앱 등록 여부)를 재확인해 버튼 모드를 갱신한다.
        private fun refreshSetup() {
            val id = currentState.detail?.challengeId ?: return
            viewModelScope.launch {
                val setup = runCatching { challengeRepository.getSetupInfo(id) }.getOrNull() ?: currentState.setup
                dispatch(
                    ChallengeDetailReducerEvent.SetupRefreshed(
                        setup = setup,
                        targetAppsRegistered = targetAppStore.isRegistered(id),
                    ),
                )
            }
            // 수동 인증 화면에서 체크하고 돌아오는 동선이 있다
            loadTodayResult(id)
            // 다른 화면에서 돌아왔을 때 방 홈이 옛 상태로 남지 않도록 함께 재조회한다.
            if (currentState.room != null) {
                loadRoom(id)
            } else {
                loadCalendar(id)
                loadMembers(id)
            }
        }

        // 비멤버/솔로의 403 등 실패는 흡수
        private fun loadRoom(
            challengeId: String,
            prefetched: ChallengeRoom? = null,
        ) {
            if (prefetched != null) {
                // Loaded 와 같은 프레임 안에서 이어 반영해 상세와 방이 함께 그려진다.
                onRoomLoaded(challengeId, prefetched)
            } else {
                viewModelScope.launch {
                    runCatching { roomRepository.getRoom(challengeId) }
                        .onSuccess { room -> onRoomLoaded(challengeId, room) }
                }
            }
            loadMembers(challengeId)
            // room·threads 는 병렬로 받는다
            loadThreads(next = false)
            // 방 안 랭킹은 랭킹 탭뿐 아니라 정보 탭 헤더의 "내 달성률" 원천이라 진입 시 함께 받는다.
            loadRanking(challengeId)
            loadCalendar(challengeId)
            loadMuteState(challengeId)
        }

        private fun onRoomLoaded(
            challengeId: String,
            room: ChallengeRoom,
        ) {
            dispatch(ChallengeDetailReducerEvent.RoomLoaded(room))
            // 방 주간 방문율의 분자.
            if (!roomViewLogged) {
                roomViewLogged = true
                bizLogger.record(
                    ChallengeEvents.roomView(
                        challengeId = challengeId,
                        myRole = room.myRole.value,
                        ownerType = room.ownerType.value,
                    ),
                )
            }
        }

        /** 이 방이 음소거인지. */
        private fun loadMuteState(challengeId: String) {
            val epoch = muteEpoch
            viewModelScope.launch {
                runCatching { notificationRepository.getSettings() }
                    .onSuccess {
                        // 조회를 보낸 뒤 사용자가 토글을 바꿨으면 늦게 도착한 응답은 버린다.
                        if (epoch != muteEpoch) return@onSuccess
                        dispatch(ChallengeDetailReducerEvent.MuteLoaded(it.isMuted(challengeId)))
                    }
            }
        }

        /** 사용자가 음소거를 바꾼 횟수. */
        private var muteEpoch = 0

        /**
         * 음소거 전환. 화면을 먼저 바꾸고 요청하며, 실패하면 되돌리고 모달로 알린다.
         * 앞 요청이 끝나기 전의 탭은 받지 않는다 — 되돌릴 기준값이 둘로 갈라진다.
         */
        private fun toggleMute(muted: Boolean) {
            val challengeId = currentState.detail?.challengeId ?: return
            if (currentState.isMuteSubmitting) return
            val before = currentState.isMuted
            // 늦게 도착한 설정 조회가 방금 바꾼 값을 덮지 않게 한다
            muteEpoch++
            dispatch(ChallengeDetailReducerEvent.MuteLoaded(muted))
            dispatch(ChallengeDetailReducerEvent.MuteSubmitting(true))
            viewModelScope.launch {
                runCatching { notificationRepository.setMuted(challengeId, muted) }
                    .onSuccess {
                        // 204 라 응답에 상태가 없다.
                        dispatch(ChallengeDetailReducerEvent.MuteSubmitting(false))
                    }.onFailure {
                        dispatch(ChallengeDetailReducerEvent.MuteLoaded(before ?: !muted))
                        emitEffect(
                            ChallengeDetailEffect.ShowErrorDialog(
                                title = "알림 설정을 바꾸지 못했어요",
                                message = it.userFacingMessage("잠시 후 다시 시도해 주세요"),
                            ),
                        )
                    }
            }
        }

        /** 솔로 상세의 월 캘린더. */
        private fun loadCalendar(challengeId: String) {
            // 내 성공·실패 캘린더라 멤버만 받는다(그룹도 ⋯ → 캘린더에서 본다)
            if (currentState.detail?.myRole?.isMember != true) return
            val month =
                currentState.calendarMonth ?: currentMonth().also {
                    dispatch(ChallengeDetailReducerEvent.CalendarMonthChanged(it))
                }
            if (currentState.isCalendarLoading) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.CalendarLoading(true))
                runCatching { roomRepository.getCalendar(challengeId, month) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.CalendarLoaded(it)) }
                    .onFailure { dispatch(ChallengeDetailReducerEvent.CalendarLoading(false)) }
            }
        }

        /** 월 이동. */
        private fun shiftCalendarMonth(offset: Long) {
            val challengeId = currentState.detail?.challengeId ?: return
            val base = currentState.calendarMonth ?: currentMonth()
            val moved =
                runCatching { YearMonth.parse(base).plusMonths(offset).toString() }.getOrNull() ?: return
            dispatch(ChallengeDetailReducerEvent.CalendarMonthChanged(moved))
            loadCalendar(challengeId)
        }

        /** 이번 달 `YYYY-MM`. */
        private fun currentMonth(): String = YearMonth.now().toString()

        /** 오늘 인증 결과(인증 모듈). */
        private fun loadTodayResult(challengeId: String) {
            viewModelScope.launch {
                runCatching { verificationRepository.getTodayResult(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.TodayResultLoaded(it)) }
            }
        }

        /** 탭 전환. */
        private fun selectTab(tab: RoomTab) {
            if (currentState.selectedTab == tab) return
            dispatch(ChallengeDetailReducerEvent.TabSelected(tab))
            // 피드가 비어 있는 채로 실패해 있었다면 탭을 여는 김에 다시 시도한다.
            if (tab == RoomTab.FEED && currentState.threads.isEmpty() && !currentState.isThreadsLoading) {
                loadThreads(next = false)
            }
            if (tab == RoomTab.RANKING) {
                ensureRankingScopeLoaded(currentState.rankingScope)
                logRankingView(currentState.rankingScope)
            }
        }

        private fun selectRankingScope(scope: RankingScope) {
            if (currentState.rankingScope == scope) return
            dispatch(ChallengeDetailReducerEvent.RankingScopeSelected(scope))
            ensureRankingScopeLoaded(scope)
            logRankingView(scope)
        }

        /** 랭킹 조회. */
        private fun logRankingView(scope: RankingScope) {
            val (viewScope, myRankNull) =
                when (scope) {
                    RankingScope.MEMBER -> RankingViewScope.IN_ROOM to (currentState.ranking?.me?.rank == null)
                    RankingScope.ROOM -> RankingViewScope.CROSS to (currentState.crossRanking?.myChallenge?.rank == null)
                }
            bizLogger.record(ChallengeEvents.rankingView(scope = viewScope, myRankNull = myRankNull))
        }

        /** 피드 페이지 로깅. */
        private fun logThreadPage(
            isFirstPage: Boolean,
            pageItemCount: Int,
        ) {
            if (isFirstPage) {
                val ownerType = currentState.room?.ownerType ?: return
                if (pageItemCount == 0 && !emptyFeedLogged) {
                    emptyFeedLogged = true
                    bizLogger.record(ChallengeEvents.roomEmptyStateView(ownerType.value))
                }
                return
            }
            bizLogger.record(
                ChallengeEvents.threadScroll(
                    // 첫 페이지가 0 번이므로 누적 개수에서 이번 페이지를 뺀 몫이 곧 페이지 번호다.
                    pageIndex =
                        ((currentState.threads.size - pageItemCount) / ThreadPolicy.PAGE_SIZE)
                            .coerceAtLeast(0),
                    itemCount = pageItemCount,
                ),
            )
        }

        private fun ensureRankingScopeLoaded(scope: RankingScope) {
            val id = currentState.detail?.challengeId ?: return
            when (scope) {
                RankingScope.MEMBER -> if (currentState.ranking == null) loadRanking(id)
                RankingScope.ROOM -> if (currentState.crossRanking == null) loadCrossRanking(next = false)
            }
        }

        /** 피드 조회. */
        private fun loadThreads(
            next: Boolean,
            retry: Boolean = false,
        ) {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            if (currentState.isThreadsLoading || currentState.isThreadsPaging) return
            // 마지막 페이지까지 받은 뒤의 추가 요청은 무시한다
            if (next && !retry && currentState.threadsCursor == null) return
            val cursor = if (next) currentState.threadsCursor else null
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.ThreadsLoading(first = cursor == null))
                runCatching { roomRepository.getThreads(id, cursor = cursor) }
                    .onSuccess {
                        dispatch(ChallengeDetailReducerEvent.ThreadsLoaded(page = it, reset = cursor == null))
                        logThreadPage(isFirstPage = cursor == null, pageItemCount = it.items.size)
                    }.onFailure { error ->
                        when (error) {
                            // 커서가 만료·변조됐다.
                            is ThreadCursorInvalidException -> {
                                dispatch(ChallengeDetailReducerEvent.ThreadsLoading(first = true))
                                runCatching { roomRepository.getThreads(id, cursor = null) }
                                    .onSuccess {
                                        dispatch(ChallengeDetailReducerEvent.ThreadsLoaded(page = it, reset = true))
                                    }.onFailure {
                                        dispatch(
                                            ChallengeDetailReducerEvent.ThreadsFailed(
                                                it.userFacingMessage("피드를 불러오지 못했어요"),
                                            ),
                                        )
                                    }
                            }

                            else ->
                                dispatch(
                                    ChallengeDetailReducerEvent.ThreadsFailed(error.userFacingMessage("피드를 불러오지 못했어요")),
                                )
                        }
                    }
            }
        }

        // 랭킹은 부가 정보라 실패해도 방 렌더를 막지 않는다
        private fun loadRanking(challengeId: String) {
            if (currentState.isRankingLoading) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.RankingLoading(true))
                runCatching { roomRepository.getRanking(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.RankingLoaded(it)) }
                    .onFailure { dispatch(ChallengeDetailReducerEvent.RankingLoading(false)) }
            }
        }

        /** 방 밖 랭킹. */
        private fun loadCrossRanking(next: Boolean) {
            val detail = currentState.detail ?: return
            if (currentState.isCrossRankingLoading) return
            val cursor = if (next) currentState.crossRanking?.nextCursor ?: return else null
            val mode = if (detail.mode.isGroup) RankingMode.GROUP else RankingMode.SOLO
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.CrossRankingLoading(true))
                runCatching {
                    roomRepository.getCrossRanking(
                        mode = mode,
                        challengeId = detail.challengeId,
                        cursor = cursor,
                    )
                }.onSuccess {
                    dispatch(ChallengeDetailReducerEvent.CrossRankingLoaded(ranking = it, append = cursor != null))
                }.onFailure { dispatch(ChallengeDetailReducerEvent.CrossRankingLoading(false)) }
            }
        }

        // 멤버 목록은 방 홈 부가 정보
        private fun loadMembers(challengeId: String) {
            viewModelScope.launch {
                runCatching { challengeRepository.getMembers(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.MembersLoaded(it)) }
            }
        }

        /** 탈퇴(본인, 방장 포함). */
        private fun leaveChallenge() {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isMemberActionLoading) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(true))
                runCatching { challengeRepository.leaveChallenge(id) }
                    .onSuccess { result ->
                        emitEffect(
                            ChallengeDetailEffect.ShowMessage(
                                if (result.penaltyApplied) "탈퇴했어요. 진행 이력이 있어 탈퇴 패널티가 적용됐어요" else "챌린지에서 나갔어요",
                            ),
                        )
                        navigationHelper.replaceStackWith(MyChallengesPage.toRoute())
                    }.onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage(it.userFacingMessage("탈퇴에 실패했어요")))
                    }
                dispatch(ChallengeDetailReducerEvent.MemberActionLoading(false))
            }
        }

        private fun openRanking() {
            val id = currentState.detail?.challengeId ?: return
            navigationHelper.navigateByRoute(ChallengeRankingPage(challengeId = id).toRoute())
        }

        /** 판정 결과 모달 확인. */
        private fun acknowledgeResult() {
            val verificationId = currentState.todayResult?.unacknowledged?.verificationId
            dispatch(ChallengeDetailReducerEvent.ResultAcknowledged)
            if (verificationId == null) return
            viewModelScope.launch {
                // 조용히 실패하고 1회만 자동 재시도한다.
                runCatching { verificationRepository.acknowledgeResult(verificationId) }
                    .onFailure { runCatching { verificationRepository.acknowledgeResult(verificationId) } }
            }
        }

        /** 오늘 실패 건 이의 제기. */
        private fun submitAppeal(
            verificationId: String,
            reason: String,
        ) {
            val challengeId = currentState.detail?.challengeId ?: currentState.challengeId
            if (currentState.isSubmittingAppeal) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.AppealReasonRejected(null))
                dispatch(ChallengeDetailReducerEvent.SubmittingAppeal(true))
                runCatching {
                    verificationRepository.submitAppeal(
                        verificationId = verificationId,
                        reason = reason,
                        imageUrl = currentState.appealImageUrl,
                    )
                }.onSuccess {
                    dispatch(ChallengeDetailReducerEvent.AppealReset)
                    emitEffect(ChallengeDetailEffect.ShowMessage("이의가 받아들여졌어요. 기록을 되돌렸어요"))
                    loadTodayResult(challengeId)
                    loadCalendar(challengeId)
                    if (currentState.detail?.mode?.isGroup == true) loadRoom(challengeId)
                }.onFailure { error ->
                    handleAppealFailure(error, challengeId)
                }
                dispatch(ChallengeDetailReducerEvent.SubmittingAppeal(false))
            }
        }

        /** 이의 제출 실패 분기. */
        private fun handleAppealFailure(
            error: Throwable,
            challengeId: String,
        ) {
            when (error) {
                is InvalidAppealReasonException ->
                    dispatch(ChallengeDetailReducerEvent.AppealReasonRejected(error.message))

                is AppealWindowClosedException -> {
                    emitEffect(ChallengeDetailEffect.ShowMessage(error.userFacingMessage("이의 신청 기한이 지났어요")))
                    // 기한이 지났으면 진입점 자체가 사라져야 한다.
                    dispatch(ChallengeDetailReducerEvent.AppealReset)
                    loadTodayResult(challengeId)
                }

                is AppealNotFailedException -> {
                    dispatch(ChallengeDetailReducerEvent.AppealReset)
                    loadTodayResult(challengeId)
                }

                else -> emitEffect(ChallengeDetailEffect.ShowMessage(error.userFacingMessage("이의를 접수하지 못했어요")))
            }
        }

        /** 수동 인증 화면으로 보낸다. */
        private fun openManualCheck() {
            val challengeId = currentState.detail?.challengeId ?: currentState.challengeId
            navigationHelper.navigateTo(VerificationManualPage(challengeId))
        }

        /** 권한 현황을 OS 에 다시 묻는다. */
        private fun refreshPermissions() {
            viewModelScope.launch {
                runCatching { permissionStatusProvider.capture() }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.PermissionsCaptured(it)) }
            }
        }

        /** 증빙 사진 업로드. */
        private fun uploadAppealImage(imageUri: String) {
            if (currentState.isUploadingAppealImage) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.AppealImageUploading(true))
                runCatching { verificationRepository.uploadAppealImage(imageUri) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.AppealImageUploaded(it)) }
                    .onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage("사진을 올리지 못했어요. 사진 없이도 제출할 수 있어요"))
                    }
                dispatch(ChallengeDetailReducerEvent.AppealImageUploading(false))
            }
        }

        private fun registerApps() {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            navigationHelper.navigateByRoute(ChallengeTargetsPage(id).toRoute())
        }

        private fun loadWatchers(challengeId: String) {
            viewModelScope.launch {
                runCatching { watcherRepository.getWatchers(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.WatchersLoaded(it)) }
            }
        }

        /** 멤버 초대 링크 발급 → 카카오톡 공유. */
        private fun inviteMember() {
            val detail = currentState.detail ?: return
            viewModelScope.launch {
                runCatching { challengeRepository.createInvitation(detail.challengeId) }
                    .onSuccess {
                        emitEffect(
                            ChallengeDetailEffect.ShareMemberInvite(
                                challengeTitle = detail.title,
                                inviteUrl = it.inviteUrl,
                            ),
                        )
                    }.onFailure {
                        emitEffect(ChallengeDetailEffect.ShowMessage(it.userFacingMessage("초대 링크를 만들지 못했어요")))
                    }
            }
        }

        private fun openWatchers() {
            val id = currentState.detail?.challengeId ?: return
            navigationHelper.navigateByRoute(ChallengeWatchersPage(id).toRoute())
        }

        /** 내 감시자 초대 생성 → 본인 카카오톡 공유. */
        private fun inviteWatcher() {
            val detail = currentState.detail ?: return
            // 공개 상세가 penalties 를 안 줄 수 있다 — 꺼진 게 확실할 때만 막고 나머지는 서버가 판단한다
            if (currentState.isInvitingWatcher || detail.penalties?.watcher == false) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.InvitingWatcher(true))
                runCatching { watcherRepository.createInvitation(detail.challengeId) }
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

        private fun registerAnchor() {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            // GPS 루틴 좌표 바인딩(verification/location) 으로 이동.
            navigationHelper.navigateByRoute(
                NavRoute(
                    AppRoutes.VERIFICATION_LOCATION,
                    mapOf(
                        "challengeId" to id,
                        "defaultRadiusM" to "500.0",
                        // ⚠️ 여기만 고정값이 남는다.
                        "dwellMinutes" to DEFAULT_DWELL_MINUTES.toString(),
                        "targetPackages" to targetAppStore.registered(id).joinToString(","),
                    ),
                ),
            )
        }

        private fun openReport(userId: String? = null) {
            if (userId == null && currentState.detail?.myRole == com.ruleup.challenge.domain.entity.MemberRole.OWNER) return
            viewModelScope.launch {
                runCatching { restrictionProvider.current() }
                    .onSuccess { restriction ->
                        if (restriction.blocks(com.ruleup.domain.entity.user.FeatureCode.REPORT)) {
                            emitEffect(ChallengeDetailEffect.ShowMessage("지금은 신고 기능을 사용할 수 없어요. 제재 내역을 확인해 주세요."))
                        } else {
                            if (userId == null) {
                                dispatch(ChallengeDetailReducerEvent.ReportSheetOpened)
                            } else {
                                dispatch(ChallengeDetailReducerEvent.UserReportSheetOpened(userId))
                            }
                        }
                    }.onFailure { emitEffect(ChallengeDetailEffect.ShowMessage("계정 상태를 확인하지 못했어요. 다시 시도해 주세요.")) }
            }
        }

        /** 챌린지 신고. */
        private fun submitReport() {
            if (currentState.reportUserId == null &&
                currentState.detail?.myRole == com.ruleup.challenge.domain.entity.MemberRole.OWNER
            ) {
                return
            }
            val challengeId = currentState.detail?.challengeId ?: return
            val reason = currentState.selectedReportReason ?: return
            // 진행 중이거나 이미 접수된 뒤면 보내지 않는다.
            if (currentState.isSubmittingReport || currentState.reportResult != null) return

            dispatch(ChallengeDetailReducerEvent.SubmittingReport(true))
            viewModelScope.launch {
                runCatching {
                    val target =
                        currentState.reportUserId?.let { userId ->
                            ReportTarget.User(userId = userId, reason = reason, context = ReportContext.ROOM, challengeId = challengeId)
                        } ?: ReportTarget.Challenge(challengeId = challengeId, reason = reason, context = ReportContext.CHALLENGE_DETAIL)
                    reportRepository.report(target)
                }.onSuccess {
                    dispatch(ChallengeDetailReducerEvent.ReportAccepted(it))
                    // 가림은 서버가 적용해 내려준다
                    load(challengeId, force = true)
                }.onFailure {
                    dispatch(ChallengeDetailReducerEvent.SubmittingReport(false))
                    emitEffect(ChallengeDetailEffect.ShowMessage(it.reportMessage()))
                    if ((it as? ReportException)?.failure == ReportFailure.ALREADY_REPORTED) {
                        dispatch(ChallengeDetailReducerEvent.ReportSheetDismissed)
                    }
                    if ((it as? ReportException)?.failure == ReportFailure.TARGET_NOT_FOUND) {
                        navigationHelper.navigateToBack()
                    }
                }
            }
        }
    }

/** 신고 실패를 사용자 문구로 옮긴다. */
private fun Throwable.reportMessage(): String =
    when ((this as? ReportException)?.failure) {
        ReportFailure.ALREADY_REPORTED -> "이미 신고한 대상이에요."
        ReportFailure.SUSPENDED -> "지금은 신고 기능을 사용할 수 없어요."
        ReportFailure.ACCOUNT_LOCKED -> "지금은 둘러보기만 할 수 있어요."
        ReportFailure.TARGET_NOT_FOUND -> "이미 사라진 챌린지예요."
        ReportFailure.NETWORK -> "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        else -> "신고를 접수하지 못했어요. 잠시 후 다시 시도해 주세요."
    }

/** 목표 체류 시간을 읽을 수 없을 때의 지오펜스 대기(분). */
private const val DEFAULT_DWELL_MINUTES = 60
