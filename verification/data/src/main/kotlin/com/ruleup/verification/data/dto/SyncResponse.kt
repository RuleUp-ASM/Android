package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.UpdatedChallenge
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.1 sync 응답 ----------
@Serializable
data class UpdatedChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("todayStatus")
    val todayStatus: String? = null,
    @SerialName("progressRate")
    val progressRate: Double? = null,
)

@Serializable
data class SyncResponse(
    @SerialName("syncedAt")
    val syncedAt: String? = null,
    @SerialName("flushIntervalSec")
    val flushIntervalSec: Int? = null,
    @SerialName("updatedChallenges")
    val updatedChallenges: List<UpdatedChallengeResponse>? = null,
    @SerialName("ignoredSignalTypes")
    val ignoredSignalTypes: List<String>? = null,
    // 한 번에 보낼 수 있는 상한(바이트). 실측 후 확정이라 서버가 아직 안 줄 수 있다
    @SerialName("maxPayloadBytes")
    val maxPayloadBytes: Int? = null,
)

internal fun SyncResponse.toDomain(): SyncResult =
    SyncResult(
        syncedAt = syncedAt.orEmpty(),
        // 명세 응답 예시 기본 1800초(30분).
        flushIntervalSec = flushIntervalSec ?: DEFAULT_FLUSH_INTERVAL_SEC,
        // 상한은 폴백을 두지 않는다 — 모르는 값을 상수로 메우면 서버가 상한을 낮춰도 알 길이 없다.
        maxPayloadBytes = maxPayloadBytes,
        updatedChallenges =
            updatedChallenges.orEmpty().mapNotNull { dto ->
                val id = dto.challengeId ?: return@mapNotNull null
                UpdatedChallenge(
                    challengeId = id,
                    todayStatus = TodayStatus.fromValue(dto.todayStatus),
                    progressRate = dto.progressRate ?: 0.0,
                )
            },
        ignoredSignalTypes = ignoredSignalTypes.orEmpty(),
    )

private const val DEFAULT_FLUSH_INTERVAL_SEC = 1800
