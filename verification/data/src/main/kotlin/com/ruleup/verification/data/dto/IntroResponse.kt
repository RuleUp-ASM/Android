package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.SignalCadence
import com.ruleup.verification.domain.entity.SyncBackoff
import com.ruleup.verification.domain.entity.SyncPolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Phase 0 인트로 응답 = 서버 정책 (전송 스펙 §0.3 settings) ----------

/** 신호별 cadence(§0.3 collection.*). */
@Serializable
data class CadenceResponse(
    @SerialName("enabled")
    val enabled: Boolean? = null,
    @SerialName("pollSec")
    val pollSec: Int? = null,
)

@Serializable
data class CollectionResponse(
    @SerialName("GEOFENCE")
    val geofence: CadenceResponse? = null,
    @SerialName("SCREEN_TIME")
    val screenTime: CadenceResponse? = null,
    @SerialName("WAKE")
    val wake: CadenceResponse? = null,
    @SerialName("HEALTH")
    val health: CadenceResponse? = null,
)

@Serializable
data class BackoffResponse(
    @SerialName("maxSec")
    val maxSec: Int? = null,
    @SerialName("factor")
    val factor: Double? = null,
)

/** 인트로 응답 = 서버 정책(§0.3 settings). */
@Serializable
data class IntroResponse(
    @SerialName("serverTimeMillis")
    val serverTimeMillis: Long? = null,
    @SerialName("flushIntervalSec")
    val flushIntervalSec: Int? = null,
    @SerialName("collection")
    val collection: CollectionResponse? = null,
    @SerialName("backoff")
    val backoff: BackoffResponse? = null,
    @SerialName("sessionId")
    val sessionId: String? = null,
)

// enabled 가 비어 오면 수집을 켠 것으로 본다 — 서버가 끄지 않은 신호를 임의로 끄면 인증이 빈다.
private fun CadenceResponse.toDomain(): SignalCadence = SignalCadence(enabled = enabled ?: true, pollSec = pollSec)

internal fun IntroResponse.toDomain(): SyncPolicy =
    SyncPolicy(
        // 미수신 시 §0.3 기본 1800초(30분).
        flushIntervalSec = flushIntervalSec ?: DEFAULT_FLUSH_INTERVAL_SEC,
        geofence = collection?.geofence?.toDomain(),
        screenTime = collection?.screenTime?.toDomain(),
        wake = collection?.wake?.toDomain(),
        health = collection?.health?.toDomain(),
        backoff =
            backoff?.let {
                SyncBackoff(
                    maxSec = it.maxSec ?: DEFAULT_BACKOFF_MAX_SEC,
                    factor = it.factor ?: DEFAULT_BACKOFF_FACTOR,
                )
            },
        sessionId = sessionId,
    )

private const val DEFAULT_FLUSH_INTERVAL_SEC = 1800
private const val DEFAULT_BACKOFF_MAX_SEC = 14400
private const val DEFAULT_BACKOFF_FACTOR = 2.0
