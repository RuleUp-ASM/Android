package com.ruleup.verification.domain.entity

import com.ruleup.domain.entity.user.AgreementType

/** 디바이스 시계/부팅 컨텍스트. */
data class DeviceClock(
    val deviceTimeMillis: Long,
    val elapsedRealtimeMillis: Long,
    val bootSessionId: String,
    val timeZone: String,
)

/** 정적 디바이스 프로필. */
data class DeviceProfile(
    val sdkInt: Int,
    val model: String,
    val lowRam: Boolean,
    val appVersion: String,
)

/** 권한 부여 상태. */
enum class PermissionState {
    GRANTED,
    DENIED,
    ;

    /** 권한이 없어 신호를 못 모으는 상태인가 */
    val isDenied: Boolean
        get() = this == DENIED
}

/** 신호별 권한 현황 스냅샷. */
data class PermissionSnapshot(
    val location: PermissionState,
    val backgroundLocation: PermissionState,
    val usageStats: PermissionState,
    val postNotifications: PermissionState,
    val healthDistance: PermissionState,
    val healthSteps: PermissionState,
    val healthSleep: PermissionState,
    val healthBackground: PermissionState,
) {
    /** 서버가 내려준 권한 토큰(`setup.requiredPermissions`)이 실제로 허용됐는가. */
    fun isGranted(token: String): Boolean? =
        when (normalizeToken(token)) {
            "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> location
            "ACCESS_BACKGROUND_LOCATION", "BACKGROUND_LOCATION" -> backgroundLocation
            "PACKAGE_USAGE_STATS", "USAGE_STATS", "SCREEN_TIME" -> usageStats
            "POST_NOTIFICATIONS", "NOTIFICATION" -> postNotifications
            "READ_DISTANCE", "HEALTH_DISTANCE" -> healthDistance
            "READ_STEPS", "HEALTH_STEPS", "HEALTH" -> healthSteps
            "READ_SLEEP", "HEALTH_SLEEP", "SLEEP" -> healthSleep
            "READ_HEALTH_DATA_IN_BACKGROUND", "HEALTH_BACKGROUND" -> healthBackground
            else -> null
        }?.let { !it.isDenied }

    companion object {
        fun normalizeToken(token: String): String =
            token.uppercase().removePrefix("ANDROID.PERMISSION.HEALTH.").removePrefix("ANDROID.PERMISSION.")

        fun requiredConsentFor(token: String): AgreementType? =
            when (normalizeToken(token)) {
                "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE",
                "ACCESS_BACKGROUND_LOCATION", "BACKGROUND_LOCATION",
                -> AgreementType.LOCATION_INFO
                "READ_DISTANCE", "HEALTH_DISTANCE", "READ_STEPS", "HEALTH_STEPS", "HEALTH",
                "READ_SLEEP", "HEALTH_SLEEP", "SLEEP",
                "READ_HEALTH_DATA_IN_BACKGROUND", "HEALTH_BACKGROUND",
                -> AgreementType.HEALTH_INFO
                else -> null
            }

        /** 이 권한을 어떻게 요청해야 하는가. */
        fun requestKindOf(token: String): PermissionRequestKind =
            when (normalizeToken(token)) {
                "PACKAGE_USAGE_STATS", "USAGE_STATS", "SCREEN_TIME" -> PermissionRequestKind.USAGE_ACCESS_SETTINGS
                "READ_DISTANCE", "HEALTH_DISTANCE", "READ_STEPS", "HEALTH_STEPS", "HEALTH",
                "READ_SLEEP", "HEALTH_SLEEP", "SLEEP",
                "READ_HEALTH_DATA_IN_BACKGROUND", "HEALTH_BACKGROUND",
                -> PermissionRequestKind.HEALTH_CONNECT
                else -> PermissionRequestKind.RUNTIME
            }
    }
}

/** 권한을 얻는 경로. */
enum class PermissionRequestKind {
    RUNTIME,
    USAGE_ACCESS_SETTINGS,
    HEALTH_CONNECT,
}

/** 신호 공백 사유. */
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

/** 신호 공백 1건. */
data class SignalGap(
    val signalType: String,
    val reason: GapReason,
    val fromMillis: Long,
    val toMillis: Long,
    val recoverable: Boolean,
)

/** VPN 게이트. */
data class NetworkState(
    val vpnActive: Boolean,
)

/** Play Integrity verdict 토큰. */
data class IntegritySnapshot(
    val token: String?,
)

/** worker heartbeat 진단. */
data class DeviceDiagnostics(
    val lastSuccessfulFlushAt: Long?,
    val standbyBucket: Int?,
    val backgroundRestricted: Boolean?,
    val isIgnoringBatteryOptimizations: Boolean?,
    val expeditedDeferred: Boolean?,
    val lastGeofenceReregisterAt: Long?,
    val hcSdkStatus: String?,
)

/** 이번 전송이 빠짐없이 담았다고 선언하는 구간. */
data class CoverageWindow(
    val from: Long,
    val until: Long,
) {
    init {
        require(until >= from) { "구간 끝이 시작보다 앞섭니다: $from..$until" }
    }

    /** 신호 일부만 실은 조각용 */
    fun emptyAtStart(): CoverageWindow = CoverageWindow(from, from)
}

/** envelope 의 신호 외 메타데이터. */
data class EnvelopeMetadata(
    val clock: DeviceClock,
    // 로그인에 쓴 기기 식별자.
    val deviceId: String,
    val activeChallengeIds: List<String>,
    val permissions: PermissionSnapshot,
    val network: NetworkState,
    val integrity: IntegritySnapshot,
    val diagnostics: DeviceDiagnostics,
    val gaps: List<SignalGap>,
    val coverage: CoverageWindow,
)
