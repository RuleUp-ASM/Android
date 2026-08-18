package com.ruleup.challenge.presentation.detail.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ChallengeNotCloneableException
import com.ruleup.challenge.domain.entity.ChallengeNotFoundException
import com.ruleup.challenge.domain.entity.DelegationAction
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.challenge.domain.entity.RankingMode
import com.ruleup.challenge.domain.entity.RoleAction
import com.ruleup.challenge.domain.entity.ThreadCursorInvalidException
import com.ruleup.challenge.domain.entity.ThreadPolicy
import com.ruleup.challenge.domain.entity.WATCHER_FREE_LIMIT
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.challenge.domain.entity.WatcherLimitExceededException
import com.ruleup.challenge.domain.navigation.ChallengeConfirmPage
import com.ruleup.challenge.domain.navigation.ChallengeNoticeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeNoticesPage
import com.ruleup.challenge.domain.navigation.ChallengeRankingPage
import com.ruleup.challenge.domain.navigation.ChallengeSettingsPage
import com.ruleup.challenge.domain.navigation.ChallengeTargetsPage
import com.ruleup.challenge.domain.observability.ChallengeEvents
import com.ruleup.challenge.domain.observability.RankingViewScope
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.challenge.domain.repository.RoomRepository
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.challenge.domain.repository.WatcherRepository
import com.ruleup.challenge.presentation.observability.ChallengeDetailTtiPage
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.TtiTracker
import com.ruleup.observability.domain.event.Channel
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
        private val challengeRepository: ChallengeRepository,
        private val roomRepository: RoomRepository,
        private val watcherRepository: WatcherRepository,
        private val exploreRepository: ExploreRepository,
        private val tokenRepository: TokenRepository,
        private val observability: Observability,
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
                ChallengeDetailIntent.Proceed -> join()
                ChallengeDetailIntent.CloneChallenge -> clone()

                ChallengeDetailIntent.OpenSettings ->
                    currentState.detail?.challengeId?.let {
                        navigationHelper.navigateByRoute(ChallengeSettingsPage(it).toRoute())
                    }
                ChallengeDetailIntent.DismissJoinBlock -> dispatch(ChallengeDetailReducerEvent.JoinBlockDismissed)
                ChallengeDetailIntent.FollowJoinBlockAction -> followJoinBlockAction()
                ChallengeDetailIntent.InviteWatcher -> inviteWatcher()
                is ChallengeDetailIntent.RemoveWatcher -> removeWatcher(intent.watcherId)
                is ChallengeDetailIntent.SelectTab -> selectTab(intent.tab)
                ChallengeDetailIntent.LoadMoreThreads -> loadThreads(next = true)
                ChallengeDetailIntent.RetryThreads -> loadThreads(next = true, retry = true)
                is ChallengeDetailIntent.SelectRankingScope -> selectRankingScope(intent.scope)
                ChallengeDetailIntent.LoadMoreCrossRanking -> loadCrossRanking(next = true)
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

                is ChallengeDetailReducerEvent.Joining -> state.copy(isJoining = event.joining)

                is ChallengeDetailReducerEvent.JoinBlocked ->
                    state.copy(isJoining = false, joinBlock = event.block)

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

                is ChallengeDetailReducerEvent.RankingLoading -> state.copy(isRankingLoading = event.loading)

                is ChallengeDetailReducerEvent.RankingLoaded ->
                    state.copy(isRankingLoading = false, ranking = event.ranking)

                is ChallengeDetailReducerEvent.CrossRankingLoading -> state.copy(isCrossRankingLoading = event.loading)

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

        /**
         * 가입. 권한은 화면이 **이 호출 전에** 확보해 둔다 — 서버는 OS 권한을 게이트로 검사하지 않고,
         * 가입 후 권한 거부를 탈퇴로 롤백하는 경로는 폐기됐다.
         */
        private fun join() {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isJoining) return
            val detail = currentState.detail
            observability.log(Channel.BUSINESS) {
                ChallengeEvents.challengeJoinAttempt(
                    challengeId = id,
                    eligible = detail?.gate?.eligible ?: false,
                    isFull = detail?.isFull ?: false,
                )
            }
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.Joining(true))
                runCatching { challengeRepository.join(id) }
                    .onSuccess { result ->
                        dispatch(ChallengeDetailReducerEvent.Joining(false))
                        // 탐색→참여 전환율의 분자. 노출·클릭과 같은 challenge_id 로 이어진다.
                        observability.log(Channel.BUSINESS) {
                            ChallengeEvents.challengeJoinResult(challengeId = id, success = true)
                        }
                        // 사이클 중간 입장이면 언제부터 판정되는지 알려준다(사이클은 1주 고정).
                        result.countFromCycle?.let {
                            emitEffect(ChallengeDetailEffect.ShowMessage("${'$'}it부터 인증이 집계돼요"))
                        }
                        load(id, force = true)
                    }.onFailure { error ->
                        observability.log(Channel.BUSINESS) {
                            ChallengeEvents.challengeJoinResult(
                                challengeId = id,
                                success = false,
                                // 게이트 차단 분포를 보려면 reason 이 곧 에러 코드다.
                                errorCode = (error as? JoinBlockedException)?.reason?.value ?: "UNKNOWN",
                            )
                        }
                        when (error) {
                            is JoinBlockedException -> {
                                // ALREADY_JOINED 는 알릴 게 없다 — 조용히 방 상세로 전환한다.
                                if (error.reason?.isAlreadyJoined == true) {
                                    dispatch(ChallengeDetailReducerEvent.Joining(false))
                                    load(id, force = true)
                                } else {
                                    dispatch(
                                        ChallengeDetailReducerEvent.JoinBlocked(
                                            JoinBlock(reason = error.reason, rejoinAvailableAt = error.rejoinAvailableAt),
                                        ),
                                    )
                                    // 정원은 수시로 변한다 — 막힌 순간의 상태를 다시 받아 뱃지를 맞춘다.
                                    if (error.reason?.needsRefresh == true) load(id, force = true)
                                }
                            }

                            is ChallengeNotFoundException -> {
                                dispatch(ChallengeDetailReducerEvent.Joining(false))
                                emitEffect(ChallengeDetailEffect.ShowMessage(error.message.orEmpty()))
                                navigationHelper.navigateToBack()
                            }

                            else -> {
                                dispatch(ChallengeDetailReducerEvent.Joining(false))
                                emitEffect(ChallengeDetailEffect.ShowMessage(error.message ?: "참여하지 못했어요"))
                            }
                        }
                    }
            }
        }

        /** 복제 → 생성 확인 화면. 초안은 생성 모듈 draft 와 동일 스키마라 확인 화면을 그대로 쓴다. */
        private fun clone() {
            val id = currentState.detail?.challengeId ?: return
            if (currentState.isCloning) return
            observability.log(Channel.BUSINESS) { ChallengeEvents.challengeCloneClick(id) }
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.Cloning(true))
                runCatching { exploreRepository.clone(id) }
                    .onSuccess {
                        dispatch(ChallengeDetailReducerEvent.Cloning(false))
                        navigationHelper.navigateTo(ChallengeConfirmPage)
                    }.onFailure { error ->
                        dispatch(ChallengeDetailReducerEvent.Cloning(false))
                        val message =
                            when (error) {
                                is ChallengeNotCloneableException -> error.message
                                is ChallengeNotFoundException -> error.message
                                else -> error.message ?: "복제하지 못했어요"
                            }
                        emitEffect(ChallengeDetailEffect.ShowMessage(message.orEmpty()))
                    }
            }
        }

        /** 차단 시트의 CTA. 사유마다 데려갈 곳이 다르다. */
        private fun followJoinBlockAction() {
            val reason = currentState.joinBlock?.reason
            dispatch(ChallengeDetailReducerEvent.JoinBlockDismissed)
            when (reason) {
                JoinBlockReason.FREE_LIMIT -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                JoinBlockReason.TIER_GATE -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_HOME))
                JoinBlockReason.CHALLENGE_COMPLETED -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_EXPLORE))
                else -> Unit
            }
        }

        // 상세 진입은 전환 분모라 화면당 1회다. 가입 후 강제 재조회에서 또 나가면 분모가 부풀어 오른다.
        private var detailViewLogged = false

        // 방 진입도 같은 이유로 화면당 1회다.
        private var roomViewLogged = false

        // 빈 피드는 같은 화면에서 여러 번 그려질 수 있어 노출 로그는 한 번만 남긴다.
        private var emptyFeedLogged = false

        private fun load(
            challengeId: String,
            force: Boolean = false,
        ) {
            // 정원·자격은 수시로 변한다 — 가입 성공·실패 직후에는 캐시를 무시하고 다시 받는다.
            if (!force && currentState.detail?.challengeId == challengeId) return
            // 현재 사용자 ID 는 멤버 목록의 "내 행" 식별용 — 실패해도 흡수(본인 한정 액션만 숨겨진다).
            if (currentState.myUserId == null) {
                viewModelScope.launch {
                    val userId = runCatching { tokenRepository.getUserId() }.getOrNull()
                    dispatch(ChallengeDetailReducerEvent.MyUserIdLoaded(userId))
                }
            }
            viewModelScope.launch {
                // 화면 진입 → 사용 가능 상태까지의 TTI. 네비게이션 시 이전 세션은 ScreenTracker 가 정리한다.
                ttiTracker.start(ChallengeDetailTtiPage, ScreenKey(AppRoutes.CHALLENGE_DETAIL))
                dispatch(ChallengeDetailReducerEvent.Loading(challengeId))
                ttiTracker.beginPhase(ChallengeDetailTtiPage, TtiTimeline.API_RESPONSE)
                runCatching { challengeRepository.getChallenge(challengeId) }
                    .onSuccess { detail ->
                        ttiTracker.endPhase(ChallengeDetailTtiPage, TtiTimeline.API_RESPONSE)
                        ttiTracker.beginPhase(ChallengeDetailTtiPage, TtiTimeline.VIEW_BINDING)
                        // 셋업 요구사항은 실패해도(미구현/멤버 아님 등) 상세 렌더를 막지 않도록 흡수한다.
                        val setup = runCatching { challengeRepository.getSetupInfo(challengeId) }.getOrNull()
                        dispatch(
                            ChallengeDetailReducerEvent.Loaded(
                                detail = detail,
                                setup = setup,
                                targetAppsRegistered = targetAppStore.isRegistered(challengeId),
                            ),
                        )
                        // 상세→참여 전환의 분모. 재조회(가입 후 force)에서는 다시 보내지 않는다.
                        if (!detailViewLogged) {
                            detailViewLogged = true
                            observability.log(Channel.BUSINESS) {
                                ChallengeEvents.challengeDetailView(
                                    challengeId = detail.challengeId,
                                    // 카드에서 넘어온 경로는 아직 라우트 인자로 전달되지 않는다(오픈 이슈).
                                    source = null,
                                    eligible = detail.gate.eligible,
                                    isFull = detail.isFull,
                                )
                            }
                        }
                        // 상세가 화면 상태로 반영된 시점 = 사용 가능. 감시자·방 홈은 부가 섹션이라 기다리지 않는다.
                        ttiTracker.endPhase(ChallengeDetailTtiPage, TtiTimeline.VIEW_BINDING)
                        ttiTracker.complete(ChallengeDetailTtiPage)
                        // 감시자는 챌린지 × 참여자 단위 — 항상 조회를 시도하고, 성공하면(=참여자)
                        // 섹션을 노출한다. 미참여 403 등 실패는 흡수(섹션 숨김).
                        loadWatchers(challengeId)
                        // 방 홈은 그룹 챌린지의 ACTIVE 멤버만 — 조회 성공 시 방 홈으로 확장 렌더링.
                        if (detail.mode.isGroup) loadRoom(challengeId)
                    }.onFailure { dispatch(ChallengeDetailReducerEvent.Failed(it.message ?: "챌린지를 불러오지 못했어요")) }
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
            // 공지 상세를 읽고 돌아오면 미읽음 수가 바뀌므로 방 홈도 함께 재조회한다.
            if (currentState.room != null) loadRoom(id)
        }

        // 비멤버/솔로의 403 등 실패는 흡수 — room 이 null 이면 기존 공개 상세 그대로 렌더링된다.
        private fun loadRoom(challengeId: String) {
            viewModelScope.launch {
                runCatching { roomRepository.getRoom(challengeId) }
                    .onSuccess { room ->
                        dispatch(ChallengeDetailReducerEvent.RoomLoaded(room))
                        // 방 주간 방문율의 분자. 재조회(가입 후 force)에서 또 나가면 분자가 부풀어 오른다.
                        if (!roomViewLogged) {
                            roomViewLogged = true
                            observability.log(Channel.BUSINESS) {
                                ChallengeEvents.roomView(
                                    challengeId = challengeId,
                                    myRole = room.myRole.value,
                                    ownerType = room.ownerType.value,
                                    hasPinnedNotice = room.pinnedNotice != null,
                                )
                            }
                        }
                    }
            }
            loadMembers(challengeId)
            // room·threads 는 병렬로 받는다 — 한쪽이 늦거나 실패해도 다른 쪽 렌더를 막지 않는다.
            loadThreads(next = false)
            // 방 안 랭킹은 랭킹 탭뿐 아니라 정보 탭 헤더의 "내 달성률" 원천이라 진입 시 함께 받는다.
            loadRanking(challengeId)
        }

        /** 탭 전환. 방 밖 랭킹처럼 진입 시 받지 않은 데이터는 처음 열릴 때 조회한다. */
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

        /** 랭킹 조회. my_rank_null 이 등재 기준(10회·50회)이 너무 높은지 판단하는 근거다. */
        private fun logRankingView(scope: RankingScope) {
            val (viewScope, myRankNull) =
                when (scope) {
                    RankingScope.MEMBER -> RankingViewScope.IN_ROOM to (currentState.ranking?.me?.rank == null)
                    RankingScope.ROOM -> RankingViewScope.CROSS to (currentState.crossRanking?.myChallenge?.rank == null)
                }
            observability.log(Channel.BUSINESS) {
                ChallengeEvents.rankingView(scope = viewScope, myRankNull = myRankNull)
            }
        }

        /**
         * 피드 페이지 로깅. 첫 페이지는 진입(room_view)에 이미 담기므로 **이어받기부터** 센다 —
         * 보려는 건 "얼마나 더 내려갔는가"이지 화면을 열었는지가 아니다.
         * 빈 피드는 봇방장 방 비중을 보려고 따로 남긴다(기능 스펙 리스크 #2).
         */
        private fun logThreadPage(
            isFirstPage: Boolean,
            pageItemCount: Int,
        ) {
            if (isFirstPage) {
                val ownerType = currentState.room?.ownerType ?: return
                if (pageItemCount == 0 && !emptyFeedLogged) {
                    emptyFeedLogged = true
                    observability.log(Channel.BUSINESS) {
                        ChallengeEvents.roomEmptyStateView(ownerType.value)
                    }
                }
                return
            }
            observability.log(Channel.BUSINESS) {
                ChallengeEvents.threadScroll(
                    // 첫 페이지가 0 번이므로 누적 개수에서 이번 페이지를 뺀 몫이 곧 페이지 번호다.
                    pageIndex =
                        ((currentState.threads.size - pageItemCount) / ThreadPolicy.PAGE_SIZE)
                            .coerceAtLeast(0),
                    itemCount = pageItemCount,
                )
            }
        }

        private fun ensureRankingScopeLoaded(scope: RankingScope) {
            val id = currentState.detail?.challengeId ?: return
            when (scope) {
                RankingScope.MEMBER -> if (currentState.ranking == null) loadRanking(id)
                RankingScope.ROOM -> if (currentState.crossRanking == null) loadCrossRanking(next = false)
            }
        }

        /**
         * 피드 조회. [next] 면 커서를 이어 받고, 아니면 첫 페이지부터 받는다.
         * [retry] 는 실패 후 재시도라 진행 중 가드(threadsError)를 통과시켜야 한다.
         */
        private fun loadThreads(
            next: Boolean,
            retry: Boolean = false,
        ) {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            if (currentState.isThreadsLoading || currentState.isThreadsPaging) return
            // 마지막 페이지까지 받은 뒤의 추가 요청은 무시한다 — 커서 없이 첫 페이지를 다시 받으면 중복된다.
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
                            // 커서가 만료·변조됐다. 이어받기를 포기하고 첫 페이지부터 다시 세운다.
                            is ThreadCursorInvalidException -> {
                                dispatch(ChallengeDetailReducerEvent.ThreadsLoading(first = true))
                                runCatching { roomRepository.getThreads(id, cursor = null) }
                                    .onSuccess {
                                        dispatch(ChallengeDetailReducerEvent.ThreadsLoaded(page = it, reset = true))
                                    }.onFailure {
                                        dispatch(
                                            ChallengeDetailReducerEvent.ThreadsFailed(
                                                it.message ?: "피드를 불러오지 못했어요",
                                            ),
                                        )
                                    }
                            }

                            else ->
                                dispatch(
                                    ChallengeDetailReducerEvent.ThreadsFailed(error.message ?: "피드를 불러오지 못했어요"),
                                )
                        }
                    }
            }
        }

        // 랭킹은 부가 정보라 실패해도 방 렌더를 막지 않는다 — 화면은 값이 없으면 빈 상태를 그린다.
        private fun loadRanking(challengeId: String) {
            if (currentState.isRankingLoading) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.RankingLoading(true))
                runCatching { roomRepository.getRanking(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.RankingLoaded(it)) }
                    .onFailure { dispatch(ChallengeDetailReducerEvent.RankingLoading(false)) }
            }
        }

        /** 방 밖 랭킹. 그룹·솔로는 서로 비교하지 않으므로 이 방의 모드로 조회한다. */
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

        // 멤버 목록은 방 홈 부가 정보 — 실패해도(권한 등) 방 홈 렌더를 막지 않도록 흡수한다.
        private fun loadMembers(challengeId: String) {
            viewModelScope.launch {
                runCatching { challengeRepository.getMembers(challengeId) }
                    .onSuccess { dispatch(ChallengeDetailReducerEvent.MembersLoaded(it)) }
            }
        }

        /** 탈퇴(본인). 성공 시 안내 후 이전 화면으로. OWNER 등 실패 사유는 서버 메시지로 노출. */
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
                runCatching { challengeRepository.delete(id) }
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
                runCatching { challengeRepository.changeMemberRole(id, userId, action) }
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
                runCatching { challengeRepository.requestDelegation(id, targetUserId) }
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
                runCatching { challengeRepository.respondDelegation(id, delegationId, DelegationAction.CANCEL) }
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
                runCatching { watcherRepository.getWatchers(challengeId) }
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

        private fun removeWatcher(watcherId: String) {
            val challengeId = currentState.detail?.challengeId ?: return
            viewModelScope.launch {
                runCatching { watcherRepository.removeWatcher(challengeId, watcherId) }
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
