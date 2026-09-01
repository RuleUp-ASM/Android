package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeOwner
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
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
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * 상세 화면 UI 테스트가 공유하는 상태 조립기.
 *
 * 화면이 읽는 축(로딩·오류·방 여부·권한·차단 사유)만 인자로 열고 나머지는 기본값으로 채운다 —
 * 테스트마다 20줄짜리 엔티티를 다시 쓰면 무엇을 바꿔서 무엇이 달라지는지가 안 보인다.
 */
internal fun detailState(
    isLoading: Boolean = false,
    detail: ChallengeDetail? = challengeDetail(),
    errorMessage: String? = null,
    setup: ChallengeSetupInfo? = null,
    permissions: PermissionSnapshot? = null,
    room: ChallengeRoom? = null,
    watchers: ChallengeWatchers? = null,
    selectedTab: RoomTab = RoomTab.INFO,
    isJoining: Boolean = false,
    isCloning: Boolean = false,
    joinBlock: JoinBlock? = null,
): ChallengeDetailState =
    ChallengeDetailState(
        challengeId = "c_1",
        isLoading = isLoading,
        detail = detail,
        errorMessage = errorMessage,
        setup = setup,
        permissions = permissions,
        room = room,
        watchers = watchers,
        selectedTab = selectedTab,
        isJoining = isJoining,
        isCloning = isCloning,
        joinBlock = joinBlock,
    )

/** 비멤버가 보는 공개 그룹 상세. */
internal fun challengeDetail(
    title: String = "아침 6시 기상",
    description: String? = "매일 아침 6시에 일어나요",
    owner: ChallengeOwner? = ChallengeOwner(userId = "u_owner", nickname = "루틴왕"),
    participantCount: Int = 3,
    capacity: Int = 10,
    cloneable: Boolean = false,
    myRole: MemberRole = MemberRole.NONE,
    joinBlockReason: JoinBlockReason? = null,
    minTier: Tier? = null,
    myDisplayTier: Tier? = null,
    requiredPermissions: List<String> = emptyList(),
): ChallengeDetail =
    ChallengeDetail(
        challengeId = "c_1",
        title = title,
        description = description,
        imageUrl = null,
        category = Category.WAKE_SLEEP,
        mode = ChallengeMode.GROUP,
        visibility = ChallengeVisibility.PUBLIC,
        status = ChallengeStatus.ACTIVE,
        owner = owner,
        ownerType = if (owner == null) OwnerType.BOT else OwnerType.USER,
        participantCount = participantCount,
        capacity = capacity,
        isFull = false,
        period = ChallengePeriod(start = "2026-09-01", end = "2026-09-30", remainingDays = 12),
        verification =
            VerificationConfig(
                type = VerificationType.AUTO,
                method = VerificationMethod.WAKE,
                detail = "기상 06:00 ±10분 내 10걸음",
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

/** 방 홈. 그룹 챌린지의 ACTIVE 멤버에게만 내려오는 값이라, 이게 있으면 화면이 3탭으로 바뀐다. */
internal fun challengeRoom(myRole: MemberRole = MemberRole.MEMBER): ChallengeRoom =
    ChallengeRoom(
        myRole = myRole,
        ownerType = OwnerType.USER,
        summary =
            RoomSummary(
                title = "아침 6시 기상",
                roomSuccessRate = null,
                remainingDays = 12,
                participantCount = 3,
                capacity = 10,
            ),
        topRanking = emptyList(),
        myTodayStatus = null,
    )

internal fun setupInfo(
    manual: Boolean = false,
    requiredPermissions: List<String> = listOf("LOCATION"),
): ChallengeSetupInfo =
    ChallengeSetupInfo(
        manual = manual,
        ready = true,
        verificationMethod = VerificationMethod.GPS_PRESENCE,
        requiredPermissions = requiredPermissions,
        requiresAnchors = true,
        anchorsConfigured = true,
        requiresTargetPackages = false,
    )

/** 모든 권한을 [state] 로 채운 스냅샷. 한 축만 바꿔 "그 권한이 꺼졌을 때"를 만든다. */
internal fun permissionSnapshot(
    all: PermissionState = PermissionState.GRANTED,
    location: PermissionState = all,
): PermissionSnapshot =
    PermissionSnapshot(
        location = location,
        backgroundLocation = all,
        activityRecognition = all,
        usageStats = all,
        postNotifications = all,
        healthDistance = all,
        healthSteps = all,
        healthSleep = all,
        healthBackground = all,
    )

/**
 * 전역 클릭 가드([com.ruleup.designsystem.SingleClickGuard])를 넘긴다.
 *
 * 가드는 `object` 라 JVM 이 사는 동안 마지막 클릭 시각을 들고 있는데, Robolectric 은
 * `SystemClock.elapsedRealtime()` 을 테스트마다 작은 값으로 되감는다. 그래서 되감긴 시각이 앞선
 * 테스트가 남긴 값보다 작아져 **첫 클릭부터 삼켜진다** — 하나만 돌리면 통과하고 클래스를 통째로
 * 돌리면 깨지는 형태로 나타난다. 시계를 단조 증가시켜 그 축을 없앤다.
 */
internal fun advanceClockPastPreviousTests() {
    ShadowSystemClock.advanceBy(Duration.ofSeconds(60))
}

/** 가드(300ms) 너머로 시계를 밀고 누른다. 한 테스트에서 두 번 이상 누를 때도 안전하다. */
internal fun SemanticsNodeInteraction.clickPastGuard(): SemanticsNodeInteraction {
    ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
    return performClick()
}
