package com.ruleup.verification.domain.entity

/**
 * sync 응답 (명세 3.1/3.3 response).
 * [nextSyncAfterSec] 로 다음 주기를 동적 조정하고, [updatedChallenges] 로 진행률 캐시를 갱신한다.
 */
data class SyncResult(
    val syncedAt: String,
    val nextSyncAfterSec: Int,
    val updatedChallenges: List<UpdatedChallenge>,
    // 미지원으로 무시된 신호 타입(디버그 로그용)
    val ignoredSignalTypes: List<String>,
)

data class UpdatedChallenge(
    val challengeId: String,
    val todayStatus: TodayStatus,
    val progressRate: Double,
)
