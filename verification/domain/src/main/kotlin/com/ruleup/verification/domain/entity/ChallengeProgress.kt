package com.ruleup.verification.domain.entity

import com.ruleup.domain.entity.category.Category
import java.time.Duration
import java.time.Instant

/**
 * 내 챌린지 진행률 일괄 조회 결과 (명세 3.2). 백그라운드 sync 가 이미 갱신한 값을 한 번에 렌더링한다.
 */
data class ProgressSnapshot(
    val asOf: String,
    val challenges: List<ChallengeProgress>,
)

/**
 * 챌린지별 진행률 (명세 3.2 challenges[]).
 *
 * [progressRate] = 성공 대상일 / 전체 대상일(%). [signalStale] 이면 "신호 미수신" 경고를 띄워
 * 권한 점검을 유도한다(명세 §6.1).
 */
data class ChallengeProgress(
    val challengeId: String,
    val title: String,
    val category: Category?,
    // SOLO / GROUP
    val participationType: String?,
    val status: String,
    val progressRate: Double,
    val successDays: Int,
    val targetDays: Int,
    val remainingDays: Int,
    val todayTarget: Boolean,
    // 모르는 값이면 null
    val todayStatus: TodayStatus?,
    val lastSyncedAt: String?,
) {
    /**
     * 신호가 끊긴 것으로 볼 만큼 sync 가 밀렸는가.
     *
     * **오늘 대상일이고, 오늘이 아직 안 끝났을 때만 참이다.**
     * - 인증하지 않는 날은 신호가 없는 게 정상이라 경고하면 멀쩡한 상태를 고장이라고 말하는 셈이다.
     * - 오늘 판정이 이미 끝난 방([TodayStatus.DONE]·[TodayStatus.FAILED])은 더 기다릴 신호가 없다.
     *   완료한 방에 "신호가 오지 않아요" 를 띄우면 사용자가 성공을 의심하게 된다(VER-10).
     * - [TodayStatus.FAIL_EXPECTED] 는 아직 뒤집을 수 있어 **경고가 가장 쓸모 있는 상태**다.
     *
     * 시각을 못 읽으면 거짓으로 둔다 — 모르는 것을 경고로 바꾸지 않는다.
     *
     * ⚠️ **수동 인증 방은 아직 가릴 수 없다.** 진행률 응답(`GET /verifications/progress`)에 인증
     * 방식이 없어, 신호를 보낼 이유가 없는 방까지 여기 들어온다. 서버가 `manual` 또는
     * `verificationMethod` 를 실어 주면 그 조건을 여기 한 줄로 더하면 된다.
     */
    fun signalStale(now: Instant): Boolean {
        if (!todayTarget) return false
        if (todayStatus != null && todayStatus !in OPEN_TODAY) return false
        val synced = lastSyncedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        return Duration.between(synced, now) > STALE_AFTER
    }

    companion object {
        /**
         * 이 시간을 넘게 신호가 없으면 경고한다.
         *
         * 주기 sync 가 30분 간격(서버 정책 기본 1800초)이라 2시간은 **4회 연속 실패**다. 더 짧게
         * 잡으면 절전으로 한두 번 밀린 정상 상태까지 경고로 읽힌다.
         */
        val STALE_AFTER: Duration = Duration.ofHours(2)

        /** 오늘 판정이 아직 안 끝난 상태. 이때만 신호를 더 기다린다. */
        private val OPEN_TODAY = setOf(TodayStatus.IN_PROGRESS, TodayStatus.FAIL_EXPECTED)
    }
}
