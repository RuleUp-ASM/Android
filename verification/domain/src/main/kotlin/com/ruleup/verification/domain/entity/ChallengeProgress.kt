package com.ruleup.verification.domain.entity

import com.ruleup.domain.entity.category.Category

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
 * [progressRate] = 성공 대상일 / 전체 대상일(%). [lastSyncedAt] 이 오래되면(예 >2h) "신호 미수신"
 * 경고 배지를 띄워 권한 점검을 유도한다(명세 §6.1).
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
    val todayStatus: TodayStatus,
    val lastSyncedAt: String?,
)
