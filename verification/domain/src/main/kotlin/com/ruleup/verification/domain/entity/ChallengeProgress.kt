package com.ruleup.verification.domain.entity

import com.ruleup.domain.entity.category.Category
import java.time.Duration
import java.time.Instant

/** 내 챌린지 진행률 일괄 조회 결과. */
data class ProgressSnapshot(
    val asOf: String,
    val challenges: List<ChallengeProgress>,
)

/** 챌린지별 진행률. */
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
    /** 신호가 끊긴 것으로 볼 만큼 sync 가 밀렸는가. */
    fun signalStale(now: Instant): Boolean {
        if (!todayTarget) return false
        if (todayStatus != null && todayStatus !in OPEN_TODAY) return false
        val synced = lastSyncedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        return Duration.between(synced, now) > STALE_AFTER
    }

    companion object {
        /** 이 시간을 넘게 신호가 없으면 경고한다. */
        val STALE_AFTER: Duration = Duration.ofHours(2)

        /** 오늘 판정이 아직 안 끝난 상태. */
        private val OPEN_TODAY = setOf(TodayStatus.IN_PROGRESS, TodayStatus.FAIL_EXPECTED)
    }
}
