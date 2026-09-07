package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaitlistPositionTest {
    @Test
    fun `순번이 아직 없으면 확인 중이다`() {
        // 이게 이 기능의 유일한 거짓말 지점이다 — null 을 1번으로 접으면 "다음 차례" 로 읽고 기다린
        // 사람이 자리를 놓친다.
        assertEquals(WaitlistPosition.Calculating, WaitlistPosition.of(null))
    }

    @Test
    fun `0 이하도 확인 중이다`() {
        // 계산 전과 구분할 근거가 없다. 1번으로 올려 그리면 위와 같은 거짓말이 된다.
        assertEquals(WaitlistPosition.Calculating, WaitlistPosition.of(0))
        assertEquals(WaitlistPosition.Calculating, WaitlistPosition.of(-1))
    }

    @Test
    fun `서버가 준 순번은 그대로 쓴다`() {
        assertEquals(WaitlistPosition.Assigned(3), WaitlistPosition.of(3))
    }

    @Test
    fun `0번 순번은 만들 수 없다`() {
        // 경계값을 타입이 막으므로 화면·테스트가 우회로 0번을 만들어 넣을 수 없다.
        assertFailsWith<IllegalArgumentException> { WaitlistPosition.Assigned(0) }
    }
}

class WaitlistPolicyTest {
    @Test
    fun `상한은 정원의 절반이고 내림한다`() {
        assertEquals(5, WaitlistPolicy.maxSize(10))
        assertEquals(3, WaitlistPolicy.maxSize(7))
    }

    @Test
    fun `정원 1명 방은 대기열이 열리지 않는다`() {
        // 상한 0 — 대기 버튼이 뜨는데 등록은 항상 409 로 막히는 상태를 만들지 않는다.
        assertEquals(0, WaitlistPolicy.maxSize(1))
    }
}

class ChallengeWaitlistTest {
    @Test
    fun `상한에 닿으면 더 받지 않는다`() {
        assertTrue(ChallengeWaitlist(waitingCount = 5, maxSize = 5, myPosition = null).isFull)
        assertFalse(ChallengeWaitlist(waitingCount = 4, maxSize = 5, myPosition = null).isFull)
    }
}

class JoinActionTest {
    @Test
    fun `자리가 있으면 그냥 참여다`() {
        assertEquals(JoinAction.JOIN, detail(isFull = false, waitlist = null).joinAction)
    }

    @Test
    fun `정원이 찼고 대기열에 여유가 있으면 대기 등록이다`() {
        val detail = detail(isFull = true, waitlist = ChallengeWaitlist(3, 5, myPosition = null))

        assertEquals(JoinAction.JOIN_WAITLIST, detail.joinAction)
    }

    @Test
    fun `대기열까지 차면 버튼 자체가 막힌다`() {
        val detail = detail(isFull = true, waitlist = ChallengeWaitlist(5, 5, myPosition = null))

        assertEquals(JoinAction.BLOCKED, detail.joinAction)
    }

    @Test
    fun `이미 대기 중이면 다시 누를 수 없다`() {
        val waitlist = ChallengeWaitlist(3, 5, myPosition = WaitlistPosition.Calculating)

        assertEquals(JoinAction.BLOCKED, detail(isFull = true, waitlist = waitlist).joinAction)
    }

    @Test
    fun `방이 시작돼 대기열이 사라지면 막힌다`() {
        // 대기열이 비워진 방은 waitlist 가 null 로 내려온다 — 오류가 아니라 정상 분기다.
        assertEquals(JoinAction.BLOCKED, detail(isFull = true, waitlist = null).joinAction)
    }

    @Test
    fun `정원 외의 차단 사유는 대기열로 우회되지 않는다`() {
        // FULL 만 대기열로 가는 사유다. 티어 미달·재입장 대기는 자리가 나도 못 들어간다.
        val waitlist = ChallengeWaitlist(0, 5, myPosition = null)
        val blocked = detail(isFull = true, waitlist = waitlist, joinBlockReason = JoinBlockReason.TIER_GATE)

        assertEquals(JoinAction.BLOCKED, blocked.joinAction)
    }
}

private fun detail(
    isFull: Boolean,
    waitlist: ChallengeWaitlist?,
    joinBlockReason: JoinBlockReason? = if (isFull) JoinBlockReason.FULL else null,
    eligible: Boolean = true,
    myRole: MemberRole = MemberRole.NONE,
) = ChallengeDetail(
    challengeId = "c1",
    title = "매일 아침 6시 기상",
    description = null,
    imageUrl = null,
    category = null,
    mode = ChallengeMode.GROUP,
    visibility = ChallengeVisibility.PUBLIC,
    status = ChallengeStatus.UPCOMING,
    owner = null,
    ownerType = OwnerType.BOT,
    participantCount = 10,
    capacity = 10,
    isFull = isFull,
    period = ChallengePeriod(start = "2026-09-01", end = "2026-09-30"),
    verification = VerificationConfig(type = VerificationType.MANUAL, method = VerificationMethod.SELF_CHECK),
    stats = ChallengeStats(completionRate = null, retentionRate = null),
    gate = ChallengeGate(minTier = null, myDisplayTier = null, eligible = eligible),
    joinBlockReason = joinBlockReason,
    rejoinAvailableAt = null,
    joinNote = JoinNote.IMMEDIATE,
    cloneable = true,
    myRole = myRole,
    moderation = null,
    waitlist = waitlist,
)
