package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.SignalCadence
import com.ruleup.verification.domain.entity.SyncBackoff
import com.ruleup.verification.domain.entity.SyncPolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Phase 0 인트로 (전송 스펙 §0.3) ----------

/** 정적 디바이스 프로필. appVersion 은 요청 최상위로 보낸다(§0.3 예시). */
@Serializable
data class DeviceProfileDto(
    @SerialName("sdkInt")
    val sdkInt: Int,
    @SerialName("model")
    val model: String,
    @SerialName("lowRam")
    val lowRam: Boolean,
)

/** 인트로 요청(§0.3). deviceProfile + appVersion + 최초 권한 스냅샷. */
@Serializable
data class IntroRequest(
    @SerialName("deviceProfile")
    val deviceProfile: DeviceProfileDto,
    @SerialName("appVersion")
    val appVersion: String,
    @SerialName("permissions")
    val permissions: PermissionsDto,
)

/** 신호별 cadence(§0.3 collection.*). */
@Serializable
data class CadenceDto(
    @SerialName("enabled")
    val enabled: Boolean = true,
    @SerialName("pollSec")
    val pollSec: Int? = null,
)

@Serializable
data class CollectionDto(
    @SerialName("GEOFENCE")
    val geofence: CadenceDto? = null,
    @SerialName("SCREEN_TIME")
    val screenTime: CadenceDto? = null,
    @SerialName("WAKE")
    val wake: CadenceDto? = null,
    @SerialName("HEALTH")
    val health: CadenceDto? = null,
)

@Serializable
data class BackoffDto(
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
    val collection: CollectionDto? = null,
    @SerialName("backoff")
    val backoff: BackoffDto? = null,
    @SerialName("sessionId")
    val sessionId: String? = null,
)

internal fun DeviceIntro.toRequest(): IntroRequest =
    IntroRequest(
        deviceProfile =
            DeviceProfileDto(
                sdkInt = profile.sdkInt,
                model = profile.model,
                lowRam = profile.lowRam,
            ),
        appVersion = profile.appVersion,
        permissions = permissions.toDto(),
    )

private fun CadenceDto.toDomain(): SignalCadence = SignalCadence(enabled = enabled, pollSec = pollSec)

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
