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
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.UnacknowledgedResult
import com.ruleup.verification.domain.entity.VerificationStreak

// 화면이 실제로 받는 모양의 상태를 만든다. 기본값은 "막히지 않는 평범한 방" 하나로 고정하고, 각 테스트는
// 자기가 검증하는 축 하나만 바꾼다 — 테스트마다 기본값이 다르면 어떤 필드가 결과를 갈랐는지 읽어낼 수 없다.

internal const val CHALLENGE_ID = "ch_1"

internal fun detail(
    title: String = "아침 6:30 기상",
    description: String? = "매일 아침 같이 일어나요",
    ownerNickname: String? = "루피",
    ownerType: OwnerType = OwnerType.USER,
    participantCount: Int = 12,
    capacity: Int = 30,
    mode: ChallengeMode = ChallengeMode.GROUP,
    verificationDetail: String? = "기상 06:00 ±10분 내 10걸음",
    requiredPermissions: List<String> = listOf("LOCATION"),
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
        category = Category.WAKE_SLEEP,
        mode = mode,
        visibility = ChallengeVisibility.PUBLIC,
        status = ChallengeStatus.ACTIVE,
        owner = ownerNickname?.let { ChallengeOwner(userId = "u_owner", nickname = it) },
        ownerType = ownerType,
        participantCount = participantCount,
        capacity = capacity,
        isFull = false,
        period = ChallengePeriod(start = "2026-07-01", end = "2026-08-11"),
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
    roomSuccessRate: Double? = 0.74,
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
    requiredPermissions: List<String> = listOf("LOCATION"),
    requiresAnchors: Boolean = false,
    anchorsConfigured: Boolean = true,
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

internal fun member(
    userId: String,
    nickname: String,
    role: MemberRole = MemberRole.MEMBER,
    tier: Tier? = Tier.SILVER,
): ChallengeMember =
    ChallengeMember(
        userId = userId,
        nickname = nickname,
        profileImageUrl = null,
        role = role,
        tier = tier,
        joinedAt = "2026-07-01T09:00:00+09:00",
    )

internal fun members(
    vararg items: ChallengeMember,
    participantCount: Int = items.size,
    capacity: Int = 30,
): ChallengeMembers =
    ChallengeMembers(
        challengeId = CHALLENGE_ID,
        participantCount = participantCount,
        capacity = capacity,
        members = items.toList(),
    )

internal fun roomUser(
    userId: String,
    nickname: String,
): RoomUser = RoomUser(userId = userId, nickname = nickname, profileImageUrl = null, blocked = false)

internal fun thread(
    id: String,
    nickname: String,
    at: String,
    type: ThreadItemType = ThreadItemType.VERIFY_SUCCESS,
    userId: String = "u_$id",
    streak: Int? = null,
    failDate: String? = null,
): ThreadItem =
    ThreadItem(
        type = type,
        id = id,
        user = roomUser(userId = userId, nickname = nickname),
        at = at,
        streak = streak,
        failDate = failDate,
    )

internal fun ranking(
    myRank: Int? = 3,
    mySuccessRate: Double? = 0.92,
    participations: Int = 24,
    items: List<RankingEntry> = emptyList(),
): ChallengeRanking =
    ChallengeRanking(
        me =
            MyRank(
                rank = myRank,
                ranked = myRank != null,
                successRate = mySuccessRate,
                participations = participations,
                gapToFirst = 0.05,
            ),
        items = items,
    )

internal fun rankEntry(
    rank: Int?,
    userId: String,
    nickname: String,
    successRate: Double?,
): RankingEntry =
    RankingEntry(
        rank = rank,
        user = roomUser(userId = userId, nickname = nickname),
        successRate = successRate,
        successCount = 20,
        participations = if (rank == null) 4 else 24,
    )

internal fun crossRanking(
    myRank: Int? = 7,
    items: List<ChallengeRankEntry> = emptyList(),
    updatedAt: String? = "2026-08-31T03:00:00+09:00",
    nextCursor: String? = null,
): CrossChallengeRanking =
    CrossChallengeRanking(
        myChallenge =
            MyChallengeRank(
                challengeId = CHALLENGE_ID,
                rank = myRank,
                ranked = myRank != null,
                successRate = 0.74,
                totalCount = 120,
            ),
        items = items,
        updatedAt = updatedAt,
        nextCursor = nextCursor,
    )

internal fun challengeRankEntry(
    rank: Int,
    challengeId: String,
    title: String,
    memberCount: Int = 12,
    totalCount: Int = 60,
    successRate: Double = 0.81,
): ChallengeRankEntry =
    ChallengeRankEntry(
        rank = rank,
        challengeId = challengeId,
        title = title,
        memberCount = memberCount,
        totalCount = totalCount,
        successRate = successRate,
    )

internal fun watchers(
    vararg items: Watcher,
    limit: Int? = 3,
): ChallengeWatchers = ChallengeWatchers(limit = limit, watchers = items.toList())

internal fun watcher(
    watcherId: String = "w_1",
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

internal fun todayResult(
    status: TodayResultStatus? = TodayResultStatus.DONE,
    verificationId: String? = "v_1",
    confirmedAt: String? = "2026-08-31T06:24:00+09:00",
    streak: VerificationStreak? = VerificationStreak(before = 6, after = 7),
    unacknowledged: UnacknowledgedResult? = null,
    appeal: AppealChance? = null,
    window: String? = null,
): TodayResult =
    TodayResult(
        date = "2026-08-31",
        verificationId = verificationId,
        status = status,
        window = window,
        confirmedAt = confirmedAt,
        failureReason = null,
        streak = streak,
        unacknowledged = unacknowledged,
        appeal = appeal,
    )

/** 전부 허용된 기기. 개별 테스트는 자기가 보는 권한 하나만 [PermissionState.DENIED] 로 내린다. */
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

/** 로딩이 끝나고 상세만 받은 상태(비멤버 공개 상세). */
internal fun loadedState(detail: ChallengeDetail = detail()): ChallengeDetailState =
    ChallengeDetailState(
        challengeId = CHALLENGE_ID,
        isLoading = false,
        detail = detail,
        errorMessage = null,
    )

/** 방 홈까지 받은 상태(그룹 ACTIVE 멤버). */
internal fun roomState(
    detail: ChallengeDetail = detail(myRole = MemberRole.MEMBER),
    room: ChallengeRoom = room(),
): ChallengeDetailState =
    loadedState(detail).copy(
        room = room,
        setup = setup(),
        permissions = permissions(),
        myUserId = "u_me",
    )
