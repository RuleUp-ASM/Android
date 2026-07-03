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
    @SerialName("nextSyncAfterSec")
    val nextSyncAfterSec: Int? = null,
    @SerialName("updatedChallenges")
    val updatedChallenges: List<UpdatedChallengeResponse>? = null,
    @SerialName("ignoredSignalTypes")
    val ignoredSignalTypes: List<String>? = null,
)

internal fun SyncResponse.toDomain(): SyncResult =
    SyncResult(
        syncedAt = syncedAt.orEmpty(),
        // 명세 응답 예시 기본 1800초(30분).
        nextSyncAfterSec = nextSyncAfterSec ?: DEFAULT_NEXT_SYNC_SEC,
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

private const val DEFAULT_NEXT_SYNC_SEC = 1800
