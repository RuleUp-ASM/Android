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

    @Test
    fun `오늘 판정이 끝난 방은 신호를 더 기다리지 않는다`() {
        // 완료한 방에 "신호가 오지 않아요" 를 띄우면 사용자가 성공을 의심하게 된다(VER-10).
        val stale = "2026-09-18T09:30:00Z"

        assertFalse(progress(lastSyncedAt = stale, todayStatus = TodayStatus.DONE).signalStale(now))
        assertFalse(progress(lastSyncedAt = stale, todayStatus = TodayStatus.FAILED).signalStale(now))
        assertFalse(progress(lastSyncedAt = stale, todayStatus = TodayStatus.NOT_TARGET).signalStale(now))
    }

    @Test
    fun `실패 예정인 방은 아직 뒤집을 수 있어 경고한다`() {
        // 경고가 가장 쓸모 있는 상태다 — 지금 권한을 고치면 오늘 판정이 바뀐다.
        assertTrue(
            progress(lastSyncedAt = "2026-09-18T09:30:00Z", todayStatus = TodayStatus.FAIL_EXPECTED)
                .signalStale(now),
        )
    }

    private fun progress(
        lastSyncedAt: String?,
        todayTarget: Boolean = true,
        todayStatus: TodayStatus? = null,
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
        todayStatus = todayStatus,
        lastSyncedAt = lastSyncedAt,
    )
}
