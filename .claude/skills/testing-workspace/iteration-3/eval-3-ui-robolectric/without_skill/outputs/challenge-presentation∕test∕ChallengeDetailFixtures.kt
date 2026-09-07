package com.ruleup.challenge.presentation.detail

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeOwner
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState

/**
 * 화면 테스트용 고정 응답. **기댓값의 출처는 화면 코드가 아니라 이 픽스처**다 —
 * 픽스처를 읽고 화면에 무엇이 떠야 하는지 손으로 계산해 단언한다.
 */
internal const val TEST_CHALLENGE_ID = "c_1"

/** 위치 권한 토큰. 서버가 `setup.requiredPermissions` 로 내려주는 값과 같은 어휘다. */
internal const val LOCATION_TOKEN = "LOCATION"

/** 사용정보 접근 토큰 — 런타임 다이얼로그로 못 받는 쪽(설정 화면 경로)이다. */
internal const val USAGE_TOKEN = "PACKAGE_USAGE_STATS"

internal fun detailFixture(
    challengeId: String = TEST_CHALLENGE_ID,
    title: String = "아침 6시 30분 기상",
    description: String? = "일어나서 물 한 잔 마시기",
    category: Category? = Category.WAKE_SLEEP,
    mode: ChallengeMode = ChallengeMode.GROUP,
    myRole: MemberRole = MemberRole.NONE,
    cloneable: Boolean = false,
    joinBlockReason: JoinBlockReason? = null,
    participantCount: Int = 4,
    capacity: Int = 10,
    ownerNickname: String? = "규칙왕",
    minTier: Tier? = null,
    myDisplayTier: Tier? = Tier.SILVER,
    verification: VerificationConfig =
        VerificationConfig(
            type = VerificationType.AUTO,
            method = VerificationMethod.GPS_PRESENCE,
            detail = "기상 06:30 ±10분 내 10걸음",
            requiredPermissions = listOf(LOCATION_TOKEN),
        ),
): ChallengeDetail =
    ChallengeDetail(
        challengeId = challengeId,
        title = title,
        description = description,
        imageUrl = null,
        category = category,
        mode = mode,
        visibility = ChallengeVisibility.PUBLIC,
        status = ChallengeStatus.ACTIVE,
        owner = ownerNickname?.let { ChallengeOwner(userId = "u_owner", nickname = it) },
        ownerType = if (ownerNickname == null) OwnerType.BOT else OwnerType.USER,
        participantCount = participantCount,
        capacity = capacity,
        isFull = false,
        period = ChallengePeriod(start = "2026-08-01", end = "2026-08-31", remainingDays = 10),
        verification = verification,
        stats = ChallengeStats(completionRate = null, retentionRate = null),
        gate = ChallengeGate(minTier = minTier, myDisplayTier = myDisplayTier, eligible = true),
        joinBlockReason = joinBlockReason,
        rejoinAvailableAt = null,
        joinNote = JoinNote.IMMEDIATE,
        cloneable = cloneable,
        myRole = myRole,
        moderation = null,
    )

internal fun setupFixture(
    manual: Boolean = false,
    requiredPermissions: List<String> = listOf(LOCATION_TOKEN),
    requiresAnchors: Boolean = false,
    anchorsConfigured: Boolean = false,
    requiresTargetPackages: Boolean = false,
): ChallengeSetupInfo =
    ChallengeSetupInfo(
        manual = manual,
        ready = true,
        verificationMethod = if (manual) VerificationMethod.SELF_CHECK else VerificationMethod.GPS_PRESENCE,
        requiredPermissions = requiredPermissions,
        requiresAnchors = requiresAnchors,
        anchorsConfigured = anchorsConfigured,
        requiresTargetPackages = requiresTargetPackages,
    )

internal fun roomFixture(
    myRole: MemberRole = MemberRole.MEMBER,
    ownerType: OwnerType = OwnerType.USER,
    remainingDays: Int = 10,
    myTodayStatus: TodayVerificationStatus? = TodayVerificationStatus.IN_PROGRESS,
): ChallengeRoom =
    ChallengeRoom(
        myRole = myRole,
        ownerType = ownerType,
        summary =
            RoomSummary(
                // 상단바 제목은 방 요약이 아니라 상세 제목에서 온다 — 값을 달리 둬 어느 쪽인지 드러낸다.
                title = "방 요약 제목",
                roomSuccessRate = 0.8,
                remainingDays = remainingDays,
                participantCount = 4,
                capacity = 10,
            ),
        topRanking = emptyList(),
        myTodayStatus = myTodayStatus,
    )

/** 내 성공률만 쓴다 — 정보 탭 헤더의 "내 달성률"과 랭킹 탭의 내 요약이 같은 값에서 온다. */
internal fun rankingFixture(mySuccessRate: Double? = 0.75): ChallengeRanking =
    ChallengeRanking(
        me =
            MyRank(
                rank = 2,
                ranked = true,
                successRate = mySuccessRate,
                participations = 12,
                gapToFirst = 0.1,
            ),
        items = emptyList(),
    )

/**
 * 기기 권한 현황. 기본은 전부 허용이고, 테스트가 필요한 항목만 꺼서 넘긴다.
 */
internal fun permissionsFixture(
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
