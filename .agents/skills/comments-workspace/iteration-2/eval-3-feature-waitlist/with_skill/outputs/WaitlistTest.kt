package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class WaitlistTest {
    @Test
    fun `순번이 없으면 확인 중이다`() {
        // 1번으로 접히면 사용자는 곧 입장한다고 믿고 앱을 계속 열어 본다.
        assertEquals(WaitlistPosition.Calculating, WaitlistPosition.of(null))
        assertEquals(WaitlistPosition.Of(1), WaitlistPosition.of(1))
    }

    @Test
    fun `대기 상한은 정원의 절반이고 홀수는 내림이다`() {
        assertEquals(5, WaitlistPolicy.maxWaiting(10))
        assertEquals(2, WaitlistPolicy.maxWaiting(5))
        // 정원 1인 방은 대기를 받지 않는다 — 상한 0 이 곧 버튼 잠금이다.
        assertEquals(0, WaitlistPolicy.maxWaiting(1))
    }

    @Test
    fun `정원이 차면 참여 대신 대기열이 된다`() {
        val action = fullRoom().joinAction(waitlist(waitingCount = 1))

        assertEquals(JoinAction.EnterWaitlist, action)
    }

    @Test
    fun `대기 상한을 채우면 버튼이 잠긴다`() {
        // 여기서 EnterWaitlist 가 나오면 눌렀다가 서버 거절을 본다.
        val action = fullRoom().joinAction(waitlist(waitingCount = 5))

        assertEquals(JoinAction.WaitlistFull, action)
    }

    @Test
    fun `대기열을 모르면 잠긴 것으로 본다`() {
        // 정원이 찼는데 대기열 조회가 실패한 경우 — 열어 두면 못 들어갈 방이 열려 보인다.
        val action = fullRoom().joinAction(waitlist = null)

        assertEquals(JoinAction.Blocked(JoinBlockReason.FULL), action)
    }

    @Test
    fun `정원 외 사유는 대기열로도 뚫리지 않는다`() {
        val banned = fullRoom().copy(joinBlockReason = JoinBlockReason.BANNED)

        assertEquals(JoinAction.Blocked(JoinBlockReason.BANNED), banned.joinAction(waitlist(waitingCount = 0)))
    }

    @Test
    fun `자리가 남으면 그냥 참여한다`() {
        val room = fullRoom().copy(participantCount = 3, isFull = false, joinBlockReason = null)

        assertEquals(JoinAction.Join, room.joinAction(waitlist(waitingCount = 0)))
    }

    private fun waitlist(waitingCount: Int) =
        ChallengeWaitlist(
            waitingCount = waitingCount,
            capacity = 10,
            myPosition = null,
        )

    private fun fullRoom() =
        ChallengeDetail(
            challengeId = "c1",
            title = "아침 6시 기상",
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
            isFull = true,
            period = ChallengePeriod(start = "2026-09-01", end = "2026-09-30"),
            verification =
                VerificationConfig(
                    type = VerificationType.MANUAL,
                    method = VerificationMethod.SELF_CHECK,
                ),
            stats = ChallengeStats(completionRate = null, retentionRate = null),
            gate = ChallengeGate(minTier = null, myDisplayTier = null, eligible = true),
            joinBlockReason = JoinBlockReason.FULL,
            rejoinAvailableAt = null,
            joinNote = JoinNote.IMMEDIATE,
            cloneable = true,
            myRole = MemberRole.NONE,
            moderation = null,
        )
}
