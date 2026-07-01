package com.ruleup.verification.domain.entity

/**
 * Phase 0 인트로 페이로드(전송 스펙 §0.3). 로그인 직후 1회 전송하는 정적 프로필 + 최초 권한 스냅샷.
 * 서버는 이를 기기 특성별 정책 산정의 입력으로 쓰고 [SyncPolicy] 를 내린다.
 */
data class DeviceIntro(
    val profile: DeviceProfile,
    val permissions: PermissionSnapshot,
)

/** 신호별 수집 cadence(전송 스펙 §0.3 collection). pollSec=null 이면 push 등 cadence 무관. */
data class SignalCadence(
    val enabled: Boolean,
    val pollSec: Int?,
)

/** 전송 실패 시 백오프 정책(전송 스펙 §0.3 backoff). */
data class SyncBackoff(
    val maxSec: Int,
    val factor: Double,
)

/**
 * 서버 정책(전송 스펙 §0.3 settings). Phase 0 응답 + 이후 sync 응답 델타로 갱신된다.
 * MVP 는 [flushIntervalSec] 만 스케줄에 적용하고 cadence/backoff 는 보관(점진 적용).
 */
data class SyncPolicy(
    val flushIntervalSec: Int,
    val geofence: SignalCadence?,
    val screenTime: SignalCadence?,
    val wake: SignalCadence?,
    val health: SignalCadence?,
    val backoff: SyncBackoff?,
    val sessionId: String?,
)
