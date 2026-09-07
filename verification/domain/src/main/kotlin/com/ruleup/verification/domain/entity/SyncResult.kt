package com.ruleup.verification.domain.entity

/**
 * sync 응답 (명세 POST /verifications/sync).
 * [flushIntervalSec] 로 다음 주기를 동적 조정하고, [updatedChallenges] 로 진행률 캐시를 갱신한다.
 *
 * [maxPayloadBytes] 는 한 번에 보낼 수 있는 상한이다. **없으면 모른다는 뜻이라 null 로 둔다** —
 * 임의의 기본값을 박으면 서버가 상한을 낮춰도 클라가 그 사실을 모른 채 계속 초과 전송한다.
 */
data class SyncResult(
    val syncedAt: String,
    val flushIntervalSec: Int,
    val maxPayloadBytes: Int?,
    val updatedChallenges: List<UpdatedChallenge>,
    // 미지원으로 무시된 신호 타입(디버그 로그용)
    val ignoredSignalTypes: List<String>,
) {
    /**
     * 413 으로 쪼개 보낸 조각들의 응답을 하나로 합친다. [other] 가 나중에 도착한 조각이다.
     *
     * 정책값(주기·상한)은 **나중 응답이 이긴다** — 서버가 조각 사이에 정책을 바꿨다면 최신이 맞다.
     * 진행 상태는 챌린지별로 합친다. 앞 조각의 갱신을 버리면 그 챌린지의 진행률 캐시가
     * 이번 sync 를 통째로 놓친다.
     */
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
        )
}

data class UpdatedChallenge(
    val challengeId: String,
    val todayStatus: TodayStatus,
    val progressRate: Double,
)
