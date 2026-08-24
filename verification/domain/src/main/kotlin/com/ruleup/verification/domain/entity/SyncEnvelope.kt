package com.ruleup.verification.domain.entity

/**
 * 디바이스 시계/부팅 컨텍스트 (전송 스펙 §0.1 공통 envelope·§6.4 시각 위조 방어).
 *
 * - [deviceTimeMillis] : `System.currentTimeMillis()` — 사용자 로컬 벽시계(조작 가능 전제).
 * - [elapsedRealtimeMillis] : `SystemClock.elapsedRealtime()` — 부팅 후 단조증가(monotonic) 교차검증용.
 * - [bootSessionId] : 부팅 세션 식별자. `bootEpoch = currentTimeMillis - elapsedRealtime` 가 바뀌면 재부팅으로 보고 새로 발급한다.
 * - [timeZone] : IANA 타임존(`Asia/Seoul`). 서버가 하루 경계(target_date) 귀속에 쓴다.
 */
data class DeviceClock(
    val deviceTimeMillis: Long,
    val elapsedRealtimeMillis: Long,
    val bootSessionId: String,
    val timeZone: String,
)

/**
 * 정적 디바이스 프로필 (전송 스펙 §0.3 Phase 0 — 로그인 1회). 주기 sync 에는 싣지 않는다.
 * 서버가 기기 특성별 정책(flushInterval·pollSec)을 정하는 입력.
 */
data class DeviceProfile(
    val sdkInt: Int,
    val model: String,
    val lowRam: Boolean,
    val appVersion: String,
)

/** 권한 부여 상태 (전송 스펙 §0.4). 권한 회수/복구로 매 flush 바뀔 수 있다. */
enum class PermissionState {
    GRANTED,
    DENIED,
    ;

    /** 권한이 없어 신호를 못 모으는 상태인가 — gap 보고의 기준. */
    val isDenied: Boolean
        get() = this == DENIED
}

/**
 * 신호별 권한 현황 스냅샷 (전송 스펙 §0.1 `permissions`). 매 flush + Phase 0 1회 전송한다.
 * 서버가 신호 부재를 NO_SIGNAL 로 처리하기 전에 "권한이 없어서인지"를 구분하는 입력.
 */
data class PermissionSnapshot(
    val location: PermissionState,
    val backgroundLocation: PermissionState,
    val activityRecognition: PermissionState,
    val usageStats: PermissionState,
    val postNotifications: PermissionState,
    val healthDistance: PermissionState,
    val healthSteps: PermissionState,
    val healthSleep: PermissionState,
    val healthBackground: PermissionState,
) {
    /**
     * 서버가 내려준 권한 토큰(`setup.requiredPermissions`)이 실제로 허용됐는가.
     *
     * **런타임 권한이 아닌 것도 여기서 판단한다** — 사용정보 접근·Health Connect 는 시스템
     * `checkSelfPermission` 으로는 알 수 없어서, 화면이 그 둘을 "요청 불가"로 흘려보내면 권한 없이
     * 참여가 성립해 버린다(매일 NO_SIGNAL_RECEIVED 로 실패한다).
     *
     * 앱이 모르는 토큰은 **null** 이다 — 서버가 토큰을 추가했을 때 그 하나 때문에 참여를 막지 않는다.
     * 모르는 것을 거부로 접으면 구버전 앱이 통째로 잠긴다.
     */
    fun isGranted(token: String): Boolean? =
        when (token.uppercase()) {
            "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> location
            "ACCESS_BACKGROUND_LOCATION", "BACKGROUND_LOCATION" -> backgroundLocation
            "ACTIVITY_RECOGNITION", "PHYSICAL_ACTIVITY" -> activityRecognition
            "PACKAGE_USAGE_STATS", "USAGE_STATS", "SCREEN_TIME" -> usageStats
            "POST_NOTIFICATIONS", "NOTIFICATION" -> postNotifications
            "READ_DISTANCE", "HEALTH_DISTANCE" -> healthDistance
            "READ_STEPS", "HEALTH_STEPS", "HEALTH" -> healthSteps
            "READ_SLEEP", "HEALTH_SLEEP", "SLEEP" -> healthSleep
            "READ_HEALTH_DATA_IN_BACKGROUND", "HEALTH_BACKGROUND" -> healthBackground
            else -> null
        }?.let { !it.isDenied }

    companion object {
        /**
         * 이 권한을 **어떻게 요청해야 하는가**(프론트엔드 테크스펙 4-4).
         *
         * 셋이 서로 다른 문을 쓴다 — 런타임은 OS 다이얼로그, 사용정보 접근은 전용 설정 화면,
         * Health Connect 는 자체 권한 컨트롤러다. 뭉뚱그리면 걸음 권한이 없는 사용자를 사용정보
         * 접근 화면으로 보내게 되고, 거기서는 아무리 켜도 원하는 권한이 생기지 않는다.
         */
        fun requestKindOf(token: String): PermissionRequestKind =
            when (token.uppercase()) {
                "PACKAGE_USAGE_STATS", "USAGE_STATS", "SCREEN_TIME" -> PermissionRequestKind.USAGE_ACCESS_SETTINGS
                "READ_DISTANCE", "HEALTH_DISTANCE", "READ_STEPS", "HEALTH_STEPS", "HEALTH",
                "READ_SLEEP", "HEALTH_SLEEP", "SLEEP",
                "READ_HEALTH_DATA_IN_BACKGROUND", "HEALTH_BACKGROUND",
                -> PermissionRequestKind.HEALTH_CONNECT
                else -> PermissionRequestKind.RUNTIME
            }
    }
}

/**
 * 권한을 얻는 경로. 화면이 어떤 문을 열어 줘야 하는지 가른다.
 *
 * [RUNTIME] 은 OS 다이얼로그로 그 자리에서 받고, 나머지 둘은 앱 밖으로 나갔다 돌아오므로 복귀 시
 * 상태를 다시 조회해야 한다.
 */
enum class PermissionRequestKind {
    RUNTIME,
    USAGE_ACCESS_SETTINGS,
    HEALTH_CONNECT,
}

/** 신호 공백 사유 (전송 스펙 §0.5). recoverable=false 면 서버 판정에서 제외, true 면 유예. */
enum class GapReason {
    PERMISSION_MISSING,
    HC_PROVIDER_UPDATE_REQUIRED,
    HC_RATE_LIMITED,
    BUFFER_EVICTED,
    USAGE_PURGED,
    GEOFENCE_NOT_REGISTERED,
    SIGNAL_UNSUPPORTED_DEVICE,
    INTEGRITY_FAILED,
}

/**
 * 신호 공백 1건 (전송 스펙 §0.5 `gaps[]`). 신호를 못 받은 구간과 사유를 서버에 알린다.
 * [signalType] 은 와이어 신호 타입 문자열(GEOFENCE/SCREEN_TIME/HEALTH/SLEEP/WAKE...).
 */
data class SignalGap(
    val signalType: String,
    val reason: GapReason,
    val fromMillis: Long,
    val toMillis: Long,
    val recoverable: Boolean,
)

/** VPN 게이트 (전송 스펙 §6.1). VPN ON 이면 서버가 위치 신호를 무효 처리한다. */
data class NetworkState(
    val vpnActive: Boolean,
)

/** Play Integrity verdict 토큰 (전송 스펙 §6.5). 미수집/미지원이면 null → 와이어에서 생략. */
data class IntegritySnapshot(
    val token: String?,
)

/**
 * worker heartbeat 진단 (전송 스펙 §0.7). 백그라운드 실행 건강성 추적용. 모두 best-effort 라 null 가능.
 */
data class DeviceDiagnostics(
    val lastSuccessfulFlushAt: Long?,
    val standbyBucket: Int?,
    val backgroundRestricted: Boolean?,
    val isIgnoringBatteryOptimizations: Boolean?,
    val expeditedDeferred: Boolean?,
    val lastGeofenceReregisterAt: Long?,
    val hcSdkStatus: String?,
)

/**
 * envelope 의 신호 외 메타데이터 (전송 스펙 §0.1). [EnvelopeMetadataProvider] 가 sync 시점에 채집한다.
 * 신호 배치([SignalBatch])와 합쳐 한 envelope 로 전송한다.
 *
 * [gaps] 는 권한 부재로 유도된 PERMISSION_MISSING 등 "계산형" 공백이다. 수집기가 적재한 버퍼형
 * 공백([SignalGap])은 유스케이스가 드레인해 합쳐 보낸다.
 */
data class EnvelopeMetadata(
    val clock: DeviceClock,
    val activeChallengeIds: List<String>,
    val permissions: PermissionSnapshot,
    val network: NetworkState,
    val integrity: IntegritySnapshot,
    val diagnostics: DeviceDiagnostics,
    val gaps: List<SignalGap>,
)
