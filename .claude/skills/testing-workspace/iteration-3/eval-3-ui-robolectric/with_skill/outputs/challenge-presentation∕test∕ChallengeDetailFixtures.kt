package com.ruleup.challenge.presentation.detail

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeOwner
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.JoinBlock
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState

/**
 * 상세 화면 UI 테스트가 공유하는 최소 픽스처.
 *
 * 값은 **명세를 만족하는 아무 값**이면 된다 — 화면이 그 값을 어떻게 그리는지가 검증 대상이지
 * 값 자체가 아니다. 그래서 Figma 목업의 더미(`D-12`·`86%`)를 베끼지 않고 테스트마다 다르게 준다.
 */
internal const val CHALLENGE_ID = "ch-1"

internal fun detail(
    title: String = "아침 6:30 기상",
    mode: ChallengeMode = ChallengeMode.GROUP,
    visibility: ChallengeVisibility? = ChallengeVisibility.PUBLIC,
    myRole: MemberRole = MemberRole.NONE,
    cloneable: Boolean = false,
    joinBlockReason: JoinBlockReason? = null,
    minTier: Tier? = null,
    myDisplayTier: Tier? = Tier.BRONZE,
    eligible: Boolean = true,
    isFull: Boolean = false,
    requiredPermissions: List<String> = emptyList(),
): ChallengeDetail =
    ChallengeDetail(
        challengeId = CHALLENGE_ID,
        title = title,
        description = "매일 아침 6시 30분에 일어나기",
        imageUrl = null,
        category = Category.WAKE_SLEEP,
        mode = mode,
        visibility = visibility,
        status = ChallengeStatus.ACTIVE,
        owner = ChallengeOwner(userId = "u-owner", nickname = "지현"),
        ownerType = OwnerType.USER,
        participantCount = 3,
        capacity = 4,
        isFull = isFull,
        period = ChallengePeriod(start = "2026-08-04", end = "2026-09-15", remainingDays = 12),
        verification =
            VerificationConfig(
                type = VerificationType.AUTO,
                method = VerificationMethod.WAKE,
                detail = "기상 06:30 ±10분 내 10걸음",
                requiredPermissions = requiredPermissions,
            ),
        stats = ChallengeStats(completionRate = null, retentionRate = null),
        gate = ChallengeGate(minTier = minTier, myDisplayTier = myDisplayTier, eligible = eligible),
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
): ChallengeRoom =
    ChallengeRoom(
        myRole = myRole,
        ownerType = ownerType,
        summary =
            RoomSummary(
                title = "아침 6:30 기상",
                roomSuccessRate = 0.8,
                remainingDays = remainingDays,
                participantCount = 3,
                capacity = 4,
            ),
        topRanking = emptyList(),
        myTodayStatus = null,
    )

internal fun ranking(successRate: Double? = 0.86): ChallengeRanking =
    ChallengeRanking(
        me =
            MyRank(
                rank = if (successRate == null) null else 1,
                ranked = successRate != null,
                successRate = successRate,
                participations = 12,
                gapToFirst = 0.0,
            ),
        items = emptyList(),
    )

/** 모든 권한이 켜진 스냅샷. 특정 권한만 끄고 싶으면 `copy` 한다. */
internal fun permissionsAllGranted(): PermissionSnapshot =
    PermissionSnapshot(
        location = PermissionState.GRANTED,
        backgroundLocation = PermissionState.GRANTED,
        activityRecognition = PermissionState.GRANTED,
        usageStats = PermissionState.GRANTED,
        postNotifications = PermissionState.GRANTED,
        healthDistance = PermissionState.GRANTED,
        healthSteps = PermissionState.GRANTED,
        healthSleep = PermissionState.GRANTED,
        healthBackground = PermissionState.GRANTED,
    )

internal fun state(
    detail: ChallengeDetail? = detail(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    room: ChallengeRoom? = null,
    selectedTab: RoomTab = RoomTab.INFO,
    ranking: ChallengeRanking? = null,
    permissions: PermissionSnapshot? = null,
    isJoining: Boolean = false,
    isCloning: Boolean = false,
    joinBlock: JoinBlock? = null,
): ChallengeDetailState =
    ChallengeDetailState(
        challengeId = CHALLENGE_ID,
        isLoading = isLoading,
        detail = detail,
        errorMessage = errorMessage,
        room = room,
        selectedTab = selectedTab,
        ranking = ranking,
        permissions = permissions,
        isJoining = isJoining,
        isCloning = isCloning,
        joinBlock = joinBlock,
    )
