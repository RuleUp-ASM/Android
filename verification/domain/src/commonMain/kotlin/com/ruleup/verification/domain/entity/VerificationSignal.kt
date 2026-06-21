package com.ruleup.verification.domain.entity

/** 지오펜스 전이 종류 (OS Geofence transitionTypes, 명세 §2.1). */
enum class GeofenceTransitionType {
    ENTER,
    EXIT,
    DWELL,
}

/** 대상 앱 전후면 전이 (UsageStats RESUMED/PAUSED/STOPPED, 명세 §2.2). */
enum class AppEventType {
    RESUMED,
    PAUSED,
    STOPPED,
}

/** 화면/잠금해제 이벤트 (WAKE 판정용, 명세 §2.2 / §3.2). */
enum class ScreenEventType {
    UNLOCK,
    SCREEN_ON,
}

/**
 * 지오펜스 전이 1건. [at] 은 epoch millis(elapsedRealtime 환산, 명세 §2.1).
 * [isMock] 은 처음부터 수집·전송한다(패널티 런칭의 하드 디펜던시, 명세 §7).
 */
data class GeofenceTransitionEvent(
    // requestId = challengeMemberId
    val requestId: String,
    val transition: GeofenceTransitionType,
    val at: Long,
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val isMock: Boolean,
)

/** 대상 앱 사용 이벤트 1건(시퀀스 그대로 전송, 누적 foregroundSec 금지, 명세 §2.2). */
data class AppUsageEvent(
    val packageName: String,
    val eventType: AppEventType,
    val at: Long,
)

/** 화면/잠금해제 이벤트 1건. */
data class ScreenEvent(
    val event: ScreenEventType,
    val at: Long,
)

/** 보조 측위 샘플 1건(sanity·mock 교차검증용, 명세 §2.3). 항상 [isMock] 포함. */
data class LocationPoint(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val isMock: Boolean,
    val at: Long,
)

/**
 * sync 페이로드의 신호 단위 (명세 §3.2). 활성 챌린지 관련 신호로만 스코핑한다.
 */
sealed interface VerificationSignal {
    val isEmpty: Boolean

    data class GeofenceTransitions(
        val events: List<GeofenceTransitionEvent>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = events.isEmpty()
    }

    data class ScreenTime(
        val appEvents: List<AppUsageEvent>,
        val screenEvents: List<ScreenEvent>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = appEvents.isEmpty() && screenEvents.isEmpty()
    }

    data class Locations(
        val points: List<LocationPoint>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = points.isEmpty()
    }
}

/**
 * 30분 배치 단위 신호 묶음. [collectedAt] 은 멱등 키(명세 §3.4) — 같은 배치 재전송은 BE 가 무시한다.
 */
data class SignalBatch(
    // ISO datetime
    val collectedAt: String,
    val signals: List<VerificationSignal>,
) {
    val isEmpty: Boolean get() = signals.all { it.isEmpty }
}

/**
 * 신호 수집 스코프 (명세 §3.2). FE 가 자기 활성 챌린지의 대상 패키지·지오펜스를 알므로
 * 그 범위 신호만 담는다(전체 앱 사용량·상시 위치 무차별 전송 금지).
 */
data class SignalScope(
    val targetPackages: Set<String>,
    // 활성 challengeMemberId(지오펜스 requestId) 집합
    val activeRequestIds: Set<String>,
)
