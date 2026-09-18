package com.ruleup.verification.domain.entity

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 신호 미수신 경고. **거짓 경고가 무경고보다 나쁘다** — 정상 상태를 고장이라 말하면 사용자가 다음부터
 * 경고를 무시하고, 정작 진짜 끊겼을 때도 넘긴다.
 */
class ChallengeProgressTest {
    private val now = Instant.parse("2026-09-18T12:00:00Z")

    @Test
    fun `대상일인데 두 시간 넘게 신호가 없으면 경고한다`() {
        // 주기 sync 가 30분이라 두 시간은 네 번 연속 실패다.
        assertTrue(progress(lastSyncedAt = "2026-09-18T09:30:00Z").signalStale(now))
    }

    @Test
    fun `두 시간 안에 받은 신호는 경고하지 않는다`() {
        // 절전으로 한두 번 밀리는 건 정상이다.
        assertFalse(progress(lastSyncedAt = "2026-09-18T10:30:00Z").signalStale(now))
    }

    @Test
    fun `인증하지 않는 날은 신호가 없어도 경고하지 않는다`() {
        // 대상일이 아니면 수집할 것 자체가 없다. 여기서 경고하면 멀쩡한 상태를 고장이라 말하게 된다.
        assertFalse(progress(lastSyncedAt = "2026-09-01T00:00:00Z", todayTarget = false).signalStale(now))
    }

    @Test
    fun `동기화 시각을 모르면 경고하지 않는다`() {
        // 모르는 것을 경고로 바꾸지 않는다 — 서버가 필드를 안 내려주면 조용히 지나간다.
        assertFalse(progress(lastSyncedAt = null).signalStale(now))
        assertFalse(progress(lastSyncedAt = "언젠가").signalStale(now))
    }

    private fun progress(
        lastSyncedAt: String?,
        todayTarget: Boolean = true,
    ) = ChallengeProgress(
        challengeId = "c_1",
        title = "매일 걷기",
        category = null,
        participationType = "SOLO",
        status = "ACTIVE",
        progressRate = 50.0,
        successDays = 5,
        targetDays = 10,
        remainingDays = 5,
        todayTarget = todayTarget,
        todayStatus = null,
        lastSyncedAt = lastSyncedAt,
    )
}
