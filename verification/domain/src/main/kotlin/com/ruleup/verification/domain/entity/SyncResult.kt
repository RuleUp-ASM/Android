package com.ruleup.verification.domain.entity

/** sync 응답. */
data class SyncResult(
    val syncedAt: String,
    val flushIntervalSec: Int,
    val maxPayloadBytes: Int?,
    val updatedChallenges: List<UpdatedChallenge>,
    // 미지원으로 무시된 신호 타입(디버그 로그용)
    val ignoredSignalTypes: List<String>,
    /** 서버가 요구한 개별 동의(LOCATION_INFO·HEALTH_INFO). */
    val consentRequired: List<String>,
) {
    /** 413 으로 쪼개 보낸 조각들의 응답을 하나로 합친다. */
    fun mergedWith(other: SyncResult): SyncResult =
        SyncResult(
            syncedAt = other.syncedAt,
            flushIntervalSec = other.flushIntervalSec,
            maxPayloadBytes = other.maxPayloadBytes ?: maxPayloadBytes,
            updatedChallenges =
                (updatedChallenges + other.updatedChallenges)
                    .associateBy { it.challengeId }
                    .values
                    .toList(),
            ignoredSignalTypes = (ignoredSignalTypes + other.ignoredSignalTypes).distinct(),
            // 한 조각만 재동의를 요구해도 그 신호는 저장되지 않는다
            consentRequired = (consentRequired + other.consentRequired).distinct(),
        )
}

data class UpdatedChallenge(
    val challengeId: String,
    // 모르는 값이면 null
    val todayStatus: TodayStatus?,
    val progressRate: Double,
)
