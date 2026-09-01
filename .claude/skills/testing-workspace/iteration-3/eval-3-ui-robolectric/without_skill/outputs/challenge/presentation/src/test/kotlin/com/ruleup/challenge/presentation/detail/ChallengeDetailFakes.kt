package com.ruleup.challenge.presentation.detail

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeThreads
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.ChallengeUpdateResult
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.DelegationAction
import com.ruleup.challenge.domain.entity.DelegationResolution
import com.ruleup.challenge.domain.entity.DelegationTicket
import com.ruleup.challenge.domain.entity.DeleteResult
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.entity.MemberRoleChange
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.OwnerClaimResult
import com.ruleup.challenge.domain.entity.RankingMode
import com.ruleup.challenge.domain.entity.RoleAction
import com.ruleup.challenge.domain.entity.RoutineDescription
import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.challenge.domain.repository.RoomRepository
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.challenge.domain.repository.WatcherRepository
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailViewModel
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.message.MessageEffect
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.TtiTracker
import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ObsContext
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.port.Clock
import com.ruleup.observability.domain.port.ContextProvider
import com.ruleup.observability.domain.port.Policy
import com.ruleup.observability.domain.port.Sink
import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.AppealHistoryItem
import com.ruleup.verification.domain.entity.AppealReceipt
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.ScreenAppSet
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import com.ruleup.verification.domain.repository.VerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * 화면 테스트가 쓰는 협력자 대역.
 *
 * 화면은 Hilt 로 주입된 실제 ViewModel 을 그대로 쓴다 — ViewModel 을 가짜로 갈면 "인텐트가 상태로,
 * 상태가 화면으로" 라는 이 화면의 유일한 배선을 건너뛰게 된다. 그래서 바꾸는 건 **서버·OS 경계**뿐이다.
 *
 * 화면이 부르지 않는 메서드는 [TODO] 로 둔다 — 나중에 화면이 그 경로를 타면 조용히 통과하지 않고 터진다.
 */
private const val UNUSED = "이 화면 테스트가 부르지 않는 경로다"

internal class FakeChallengeRepository(
    var detail: ChallengeDetail? = detailFixture(),
    var detailError: Throwable? = null,
    var setup: ChallengeSetupInfo? = null,
    var setupError: Throwable? = null,
    var members: ChallengeMembers? = null,
    var joinError: Throwable? = null,
) : ChallengeRepository {
    var joinCount: Int = 0
        private set

    override suspend fun getChallenge(challengeId: String): ChallengeDetail {
        detailError?.let { throw it }
        return detail ?: throw IllegalStateException("detail 픽스처가 없다")
    }

    override suspend fun getSetupInfo(challengeId: String): ChallengeSetupInfo {
        setupError?.let { throw it }
        // 서버가 setup 을 못 주는 경우(미구현·비멤버)를 그대로 재현한다 — ViewModel 이 흡수한다.
        return setup ?: throw IllegalStateException("setup 없음")
    }

    override suspend fun getMembers(challengeId: String): ChallengeMembers =
        members ?: throw IllegalStateException("멤버 목록 없음(403)")

    override suspend fun join(challengeId: String): JoinResult {
        joinCount++
        joinError?.let { throw it }
        return JoinResult(countFromCycle = null, requiredPermissions = emptyList(), personalSetupRequired = false)
    }

    override suspend fun getRoutineTemplates(): List<RoutineTemplate> = TODO(UNUSED)

    override suspend fun createDraft(description: RoutineDescription): DraftResult = TODO(UNUSED)

    override suspend fun createDraftFromTemplate(templateId: Long): DraftResult.Ok = TODO(UNUSED)

    override suspend fun create(
        command: CreateChallengeCommand,
        idempotencyKey: String,
    ): CreatedChallenge = TODO(UNUSED)

    override suspend fun uploadImage(imageUri: String): String = TODO(UNUSED)

    override suspend fun getSettings(challengeId: String): ChallengeSettings = TODO(UNUSED)

    override suspend fun update(
        challengeId: String,
        update: ChallengeUpdate,
    ): ChallengeUpdateResult = TODO(UNUSED)

    override suspend fun delete(challengeId: String): DeleteResult = TODO(UNUSED)

    override suspend fun getMyChallenges(): List<MyChallenge> = TODO(UNUSED)

    override suspend fun leaveChallenge(challengeId: String): LeaveResult = TODO(UNUSED)

    override suspend fun changeMemberRole(
        challengeId: String,
        userId: String,
        action: RoleAction,
    ): MemberRoleChange = TODO(UNUSED)

    override suspend fun requestDelegation(
        challengeId: String,
        targetUserId: String,
    ): DelegationTicket = TODO(UNUSED)

    override suspend fun respondDelegation(
        challengeId: String,
        delegationId: String,
        action: DelegationAction,
    ): DelegationResolution = TODO(UNUSED)

    override suspend fun claimOwner(challengeId: String): OwnerClaimResult = TODO(UNUSED)
}

internal class FakeRoomRepository(
    var room: ChallengeRoom? = null,
    var threads: ChallengeThreads = ChallengeThreads(items = emptyList(), nextCursor = null),
    var ranking: ChallengeRanking? = null,
) : RoomRepository {
    override suspend fun getRoom(challengeId: String): ChallengeRoom =
        room ?: throw IllegalStateException("방 홈 없음(비멤버 403)")

    override suspend fun getThreads(
        challengeId: String,
        cursor: String?,
        size: Int,
    ): ChallengeThreads = threads

    override suspend fun getRanking(challengeId: String): ChallengeRanking =
        ranking ?: throw IllegalStateException("랭킹 없음")

    override suspend fun getCrossRanking(
        mode: RankingMode,
        challengeId: String?,
        cursor: String?,
        size: Int?,
    ): CrossChallengeRanking = throw IllegalStateException("방 순위 없음")
}

/** 감시자 조회가 실패하면 섹션이 통째로 숨는다 — 기본값은 "미참여" 쪽이다. */
internal class FakeWatcherRepository(
    var watchers: ChallengeWatchers? = null,
) : WatcherRepository {
    override suspend fun getWatchers(challengeId: String): ChallengeWatchers =
        watchers ?: throw IllegalStateException("감시자 조회 실패(403)")

    override suspend fun createInvitation(challengeId: String): WatcherInvitation = TODO(UNUSED)

    override suspend fun removeWatcher(
        challengeId: String,
        watcherId: String,
    ) = TODO(UNUSED)
}

internal class FakeVerificationRepository(
    var todayResult: TodayResult? = null,
) : VerificationRepository {
    override suspend fun getTodayResult(challengeId: String): TodayResult =
        todayResult ?: throw IllegalStateException("오늘 인증 결과 없음")

    override suspend fun submitIntro(intro: DeviceIntro): SyncPolicy = TODO(UNUSED)

    override suspend fun sync(
        metadata: EnvelopeMetadata,
        batch: SignalBatch,
    ): SyncResult = TODO(UNUSED)

    override suspend fun getProgress(filter: ProgressFilter): ProgressSnapshot = TODO(UNUSED)

    override suspend fun setupChallenge(
        challengeId: String,
        anchors: AnchorSet,
        targetPackages: List<String>,
    ): ChallengeSetupResult = TODO(UNUSED)

    override suspend fun getMyLocation(challengeId: String): MyLocation? = TODO(UNUSED)

    override suspend fun updateMyLocation(
        challengeId: String,
        anchors: AnchorSet,
    ): MyLocation = TODO(UNUSED)

    override suspend fun getMyScreenApps(challengeId: String): MyScreenApps? = TODO(UNUSED)

    override suspend fun updateMyScreenApps(
        challengeId: String,
        apps: ScreenAppSet,
    ): ScreenAppsUpdate = TODO(UNUSED)

    override suspend fun submitAppeal(
        verificationId: String,
        reason: String,
        imageUrl: String?,
    ): AppealReceipt = TODO(UNUSED)

    override suspend fun acknowledgeResult(verificationId: String) = TODO(UNUSED)

    override suspend fun cancelManual(verificationId: String) = TODO(UNUSED)

    override suspend fun uploadAppealImage(imageUri: String): String = TODO(UNUSED)

    override suspend fun getMyAppeals(): List<AppealHistoryItem> = TODO(UNUSED)

    override suspend fun submitManual(
        challengeId: String,
        targetDate: String?,
        note: String?,
    ): ManualSubmitResult = TODO(UNUSED)

    override suspend fun searchPlaces(
        query: String,
        lat: Double?,
        lng: Double?,
        radiusM: Int?,
    ): List<Place> = TODO(UNUSED)

    override suspend fun reverseGeocode(
        lat: Double,
        lng: Double,
    ): Place? = TODO(UNUSED)
}

internal class FakeExploreRepository : ExploreRepository {
    override suspend fun getTrending(category: Category?): TrendingSnapshot = TODO(UNUSED)

    override suspend fun getCategories(): List<ChallengeCategoryCount> = TODO(UNUSED)

    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult = TODO(UNUSED)

    override suspend fun clone(challengeId: String): DraftResult.Ok = TODO(UNUSED)
}

/** 앱 등록 여부는 로컬 판정이다 — 서버가 아니라 이 스토어가 CTA 단계를 가른다. */
internal class FakeTargetAppStore(
    private var packages: List<String> = emptyList(),
) : TargetAppStore {
    override fun isRegistered(challengeId: String): Boolean = packages.isNotEmpty()

    override fun registered(challengeId: String): List<String> = packages

    override fun save(
        challengeId: String,
        packages: List<String>,
    ) {
        this.packages = packages
    }
}

internal class FakeTokenRepository(
    private val storedUserId: String? = "u_me",
) : TokenRepository {
    override suspend fun getUserId(): String? = storedUserId

    override val userId: Flow<String?> = flowOf(storedUserId)

    override val isLoggedIn: Flow<Boolean> = flowOf(true)

    override suspend fun saveSession(
        token: Token,
        userId: String,
    ) = TODO(UNUSED)

    override suspend fun saveTokens(
        token: Token,
        userId: String?,
    ) = TODO(UNUSED)

    override suspend fun getAccessToken(): String? = null

    override fun cachedAccessToken(): String? = null

    override suspend fun getRefreshToken(): String? = null

    override suspend fun clear() = TODO(UNUSED)

    override suspend fun hasEverLoggedIn(): Boolean = true
}

internal class RecordingNavigationHelper : NavigationHelper {
    val routes = mutableListOf<NavRoute>()
    var backCount = 0
        private set

    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateByRoute(route: NavRoute) {
        routes += route
    }

    override fun navigateTo(page: Page) {
        routes += page.toRoute()
    }

    override fun replaceStackWith(route: NavRoute) {
        routes += route
    }

    override fun navigateToBack() {
        backCount++
    }
}

internal class RecordingMessageHelper : MessageHelper {
    val toasts = mutableListOf<String>()

    override val effect: Flow<MessageEffect> = emptyFlow()

    override fun showToast(toastMsg: String) {
        toasts += toastMsg
    }

    override fun showSnackBar(messageText: String) {
        toasts += messageText
    }

    override fun showSnackBar(messageRes: Int) = Unit

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) = Unit
}

/**
 * 관측은 화면 검증 대상이 아니다. 정책을 꺼 두면 페이로드가 **만들어지지도 않아**,
 * 이벤트 카탈로그가 바뀌어도 화면 테스트가 덩달아 깨지지 않는다.
 */
private object ZeroClock : Clock {
    override fun epochMillis(): Long = 0L

    override fun monotonicNanos(): Long = 0L
}

private fun silentObservability(): Observability =
    Observability(
        clock = ZeroClock,
        contextProvider = ContextProvider { ObsContext(currentScreen = null) },
        profile = BuildProfile.PRODUCTION,
        policy =
            Policy { _: Channel, _: Severity, _: String? -> false },
        sink =
            object : Sink {
                override fun emit(event: ObsEvent) = Unit
            },
    )

/**
 * 화면 하나를 세울 때 필요한 경계 대역 묶음. 테스트는 필요한 것만 갈아 끼우고 [viewModel] 을 화면에 준다.
 *
 * [permissions] 가 null 이면 권한 현황 조회 실패다 — "아직 못 물었다" 와 "꺼져 있다" 는 화면에서
 * 다르게 취급돼야 해서 둘을 구분해 둔다.
 */
internal class ChallengeDetailEnv(
    val challengeRepository: FakeChallengeRepository = FakeChallengeRepository(),
    val roomRepository: FakeRoomRepository = FakeRoomRepository(),
    val watcherRepository: FakeWatcherRepository = FakeWatcherRepository(),
    val verificationRepository: FakeVerificationRepository = FakeVerificationRepository(),
    val exploreRepository: FakeExploreRepository = FakeExploreRepository(),
    val tokenRepository: FakeTokenRepository = FakeTokenRepository(),
    val targetAppStore: FakeTargetAppStore = FakeTargetAppStore(),
    val navigation: RecordingNavigationHelper = RecordingNavigationHelper(),
    val messages: RecordingMessageHelper = RecordingMessageHelper(),
    val permissions: PermissionSnapshot? = permissionsFixture(),
) {
    private val permissionStatusProvider =
        object : PermissionStatusProvider {
            override suspend fun capture(): PermissionSnapshot =
                permissions ?: throw IllegalStateException("권한 현황을 조회하지 못했다")
        }

    private val observability = silentObservability()

    val viewModel: ChallengeDetailViewModel by lazy {
        ChallengeDetailViewModel(
            challengeRepository = challengeRepository,
            roomRepository = roomRepository,
            watcherRepository = watcherRepository,
            verificationRepository = verificationRepository,
            permissionStatusProvider = permissionStatusProvider,
            exploreRepository = exploreRepository,
            tokenRepository = tokenRepository,
            observability = observability,
            targetAppStore = targetAppStore,
            navigationHelper = navigation,
            ttiTracker = TtiTracker(clock = ZeroClock, observability = observability),
        )
    }
}
