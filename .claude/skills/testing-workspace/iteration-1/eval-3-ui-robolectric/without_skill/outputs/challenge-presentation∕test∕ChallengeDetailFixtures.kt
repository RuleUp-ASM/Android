package com.ruleup.challenge.presentation.detail

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMember
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeOwner
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeRankEntry
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyChallengeRank
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RankingEntry
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.RoomUser
import com.ruleup.challenge.domain.entity.ThreadItem
import com.ruleup.challenge.domain.entity.ThreadItemType
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherChannel
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.domain.entity.WatcherType
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.verification.domain.entity.AppealChance
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.UnacknowledgedResult
import com.ruleup.verification.domain.entity.VerificationStreak

// 화면이 읽는 값만 인자로 열어 둔다. 나머지는 어느 테스트에서도 표시에 관여하지 않는 배경값이다.

internal const val CHALLENGE_ID = "ch-1"

internal fun detail(
    title: String = "아침 6:30 기상",
    description: String? = "매일 아침 같이 일어나요",
    category: Category? = Category.WAKE_SLEEP,
    mode: ChallengeMode = ChallengeMode.GROUP,
    owner: ChallengeOwner? = ChallengeOwner(userId = "u-owner", nickname = "홍길동"),
    ownerType: OwnerType = OwnerType.USER,
    participantCount: Int = 12,
    capacity: Int = 30,
    verificationDetail: String? = "기상 06:00 ±10분 내 10걸음",
    requiredPermissions: List<String> = emptyList(),
    joinBlockReason: JoinBlockReason? = null,
    cloneable: Boolean = false,
    myRole: MemberRole = MemberRole.NONE,
    minTier: Tier? = null,
    myDisplayTier: Tier? = Tier.BRONZE,
): ChallengeDetail =
    ChallengeDetail(
        challengeId = CHALLENGE_ID,
        title = title,
        description = description,
        imageUrl = null,
        category = category,
        mode = mode,
        visibility = ChallengeVisibility.PUBLIC,
        status = ChallengeStatus.ACTIVE,
        owner = owner,
        ownerType = ownerType,
        participantCount = participantCount,
        capacity = capacity,
        isFull = false,
        period = ChallengePeriod(start = "2026-08-01", end = "2026-08-28"),
        verification =
            VerificationConfig(
                type = VerificationType.AUTO,
                method = VerificationMethod.WAKE,
                detail = verificationDetail,
                requiredPermissions = requiredPermissions,
            ),
        stats = ChallengeStats(completionRate = null, retentionRate = null),
        gate = ChallengeGate(minTier = minTier, myDisplayTier = myDisplayTier, eligible = true),
        joinBlockReason = joinBlockReason,
        rejoinAvailableAt = null,
        joinNote = JoinNote.IMMEDIATE,
        cloneable = cloneable,
        myRole = myRole,
        moderation = null,
    )

internal fun room(
    myRole: MemberRole = MemberRole.MEMBER,
    ownerType: OwnerType = OwnerType.USER,
    remainingDays: Int = 12,
    participantCount: Int = 12,
    capacity: Int = 30,
    roomSuccessRate: Double? = 0.8,
    myTodayStatus: TodayVerificationStatus? = TodayVerificationStatus.DONE,
): ChallengeRoom =
    ChallengeRoom(
        myRole = myRole,
        ownerType = ownerType,
        summary =
            RoomSummary(
                title = "아침 6:30 기상",
                roomSuccessRate = roomSuccessRate,
                remainingDays = remainingDays,
                participantCount = participantCount,
                capacity = capacity,
            ),
        topRanking = emptyList(),
        myTodayStatus = myTodayStatus,
    )

internal fun setup(
    manual: Boolean = false,
    requiredPermissions: List<String> = emptyList(),
    requiresAnchors: Boolean = false,
    anchorsConfigured: Boolean = false,
    requiresTargetPackages: Boolean = false,
): ChallengeSetupInfo =
    ChallengeSetupInfo(
        manual = manual,
        ready = true,
        verificationMethod = VerificationMethod.WAKE,
        requiredPermissions = requiredPermissions,
        requiresAnchors = requiresAnchors,
        anchorsConfigured = anchorsConfigured,
        requiresTargetPackages = requiresTargetPackages,
    )

internal fun members(
    participantCount: Int = 3,
    capacity: Int = 30,
    members: List<ChallengeMember> =
        listOf(
            member(userId = "u-owner", nickname = "홍길동", role = MemberRole.OWNER),
            member(userId = "u-manager", nickname = "김관리", role = MemberRole.MANAGER),
            member(userId = "u-me", nickname = "나나", role = MemberRole.MEMBER),
        ),
): ChallengeMembers =
    ChallengeMembers(
        challengeId = CHALLENGE_ID,
        participantCount = participantCount,
        capacity = capacity,
        members = members,
    )

internal fun member(
    userId: String,
    nickname: String,
    role: MemberRole,
    tier: Tier? = Tier.SILVER,
): ChallengeMember =
    ChallengeMember(
        userId = userId,
        nickname = nickname,
        profileImageUrl = null,
        role = role,
        tier = tier,
        joinedAt = "2026-08-01T09:00:00+09:00",
    )

internal fun watchers(
    limit: Int? = 3,
    watchers: List<Watcher> = listOf(watcher()),
): ChallengeWatchers = ChallengeWatchers(limit = limit, watchers = watchers)

internal fun watcher(
    watcherId: String = "w-1",
    displayName: String? = "엄마",
    status: WatcherStatus = WatcherStatus.ACTIVE,
): Watcher =
    Watcher(
        watcherId = watcherId,
        type = WatcherType.USER,
        channel = WatcherChannel.IN_APP,
        status = status,
        displayName = displayName,
        contactMasked = null,
        expiresAt = null,
    )

internal fun ranking(
    myRank: Int? = 2,
    mySuccessRate: Double? = 0.75,
    participations: Int = 20,
    items: List<RankingEntry> =
        listOf(
            rankingEntry(rank = 1, userId = "u-owner", nickname = "홍길동", successRate = 0.9),
            rankingEntry(rank = 2, userId = "u-me", nickname = "나나", successRate = 0.75),
            rankingEntry(rank = null, userId = "u-new", nickname = "새싹", successRate = null),
        ),
): ChallengeRanking =
    ChallengeRanking(
        me =
            MyRank(
                rank = myRank,
                ranked = myRank != null,
                successRate = mySuccessRate,
                participations = participations,
                gapToFirst = 0.15,
            ),
        items = items,
    )

internal fun rankingEntry(
    rank: Int?,
    userId: String,
    nickname: String,
    successRate: Double?,
): RankingEntry =
    RankingEntry(
        rank = rank,
        user = roomUser(userId = userId, nickname = nickname),
        successRate = successRate,
        successCount = 15,
        participations = 20,
    )

internal fun crossRanking(
    myRank: Int? = 4,
    items: List<ChallengeRankEntry> =
        listOf(
            ChallengeRankEntry(
                rank = 1,
                challengeId = "ch-other",
                title = "새벽 러닝",
                memberCount = 20,
                totalCount = 300,
                successRate = 0.95,
            ),
            ChallengeRankEntry(
                rank = 4,
                challengeId = CHALLENGE_ID,
                title = "아침 6:30 기상",
                memberCount = 12,
                totalCount = 120,
                successRate = 0.8,
            ),
        ),
    nextCursor: String? = null,
): CrossChallengeRanking =
    CrossChallengeRanking(
        myChallenge =
            MyChallengeRank(
                challengeId = CHALLENGE_ID,
                rank = myRank,
                ranked = myRank != null,
                successRate = 0.8,
                totalCount = 120,
            ),
        items = items,
        updatedAt = "2026-08-30T03:00:00+09:00",
        nextCursor = nextCursor,
    )

internal fun roomUser(
    userId: String,
    nickname: String,
): RoomUser =
    RoomUser(
        userId = userId,
        nickname = nickname,
        profileImageUrl = null,
        blocked = false,
    )

internal fun successThread(
    id: String = "t-1",
    nickname: String = "홍길동",
    userId: String = "u-owner",
    at: String = "2026-07-25T06:12:00+09:00",
    streak: Int? = 3,
): ThreadItem =
    ThreadItem(
        type = ThreadItemType.VERIFY_SUCCESS,
        id = id,
        user = roomUser(userId = userId, nickname = nickname),
        at = at,
        streak = streak,
        failDate = null,
    )

internal fun failThread(
    id: String = "t-2",
    nickname: String = "김관리",
    userId: String = "u-manager",
    at: String = "2026-07-25T09:00:00+09:00",
    failDate: String? = "2026-07-24",
): ThreadItem =
    ThreadItem(
        type = ThreadItemType.VERIFY_FAIL,
        id = id,
        user = roomUser(userId = userId, nickname = nickname),
        at = at,
        streak = null,
        failDate = failDate,
    )

internal fun todayResult(
    status: TodayResultStatus? = TodayResultStatus.DONE,
    verificationId: String? = "v-1",
    window: String? = "06:00-07:00",
    confirmedAt: String? = "2026-08-31T06:24:00+09:00",
    failureReason: FailureReason? = null,
    streak: VerificationStreak? = VerificationStreak(before = 6, after = 7),
    unacknowledged: UnacknowledgedResult? = null,
    appeal: AppealChance? = null,
): TodayResult =
    TodayResult(
        date = "2026-08-31",
        verificationId = verificationId,
        status = status,
        window = window,
        confirmedAt = confirmedAt,
        failureReason = failureReason,
        streak = streak,
        unacknowledged = unacknowledged,
        appeal = appeal,
    )

/** 모든 권한이 허용된 기기. 특정 권한만 꺼서 "끊긴 권한" 경로를 만든다. */
internal fun permissions(
    location: PermissionState = PermissionState.GRANTED,
    usageStats: PermissionState = PermissionState.GRANTED,
): PermissionSnapshot =
    PermissionSnapshot(
        location = location,
        backgroundLocation = PermissionState.GRANTED,
        activityRecognition = PermissionState.GRANTED,
        usageStats = usageStats,
        postNotifications = PermissionState.GRANTED,
        healthDistance = PermissionState.GRANTED,
        healthSteps = PermissionState.GRANTED,
        healthSleep = PermissionState.GRANTED,
        healthBackground = PermissionState.GRANTED,
    )

/** 로딩이 끝나고 상세가 도착한 상태. 각 테스트는 여기서 필요한 필드만 바꾼다. */
internal fun loadedState(
    detail: ChallengeDetail? = detail(),
    room: ChallengeRoom? = null,
): ChallengeDetailState =
    ChallengeDetailState(
        challengeId = CHALLENGE_ID,
        isLoading = false,
        detail = detail,
        errorMessage = null,
        room = room,
    )
