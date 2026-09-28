package com.ruleup.verification.domain.entity

/** Phase 0 인트로 페이로드. */
data class DeviceIntro(
    val profile: DeviceProfile,
    val permissions: PermissionSnapshot,
)

/** 신호별 수집 cadence. */
data class SignalCadence(
    val enabled: Boolean,
    val pollSec: Int?,
)

/** 전송 실패 시 백오프 정책. */
data class SyncBackoff(
    val maxSec: Int,
    val factor: Double,
)

/** 서버 정책. */
data class SyncPolicy(
    val flushIntervalSec: Int,
    val geofence: SignalCadence?,
    val screenTime: SignalCadence?,
    val wake: SignalCadence?,
    val health: SignalCadence?,
    val backoff: SyncBackoff?,
    val sessionId: String?,
)
