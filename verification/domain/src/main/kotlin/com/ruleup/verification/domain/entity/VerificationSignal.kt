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

/** 움직임(HEALTH) 지표 (명세 §3·§8). Health Connect 에서 읽어 결과값만 올린다(원시 트랙 미전송). */
enum class HealthMetric {
    DISTANCE,
    STEPS,
    EXERCISE_DURATION,
}

/**
 * 기록 방식 (Health Connect Metadata.recordingMethod, 명세 §8.2 신뢰 게이트).
 * [MANUAL] 은 손입력이라 BE 가 거부한다. 미인식은 [UNKNOWN].
 */
enum class RecordingMethod {
    AUTO,
    ACTIVE,
    MANUAL,
    UNKNOWN,
}

/** 기록 디바이스 (Health Connect Metadata.device.type, 명세 §8.2 — WATCH 신뢰 가중↑). */
enum class HealthDeviceType {
    PHONE,
    WATCH,
    UNKNOWN,
}

/**
 * 움직임 신호의 신뢰 메타데이터(명세 §6.2·§8.2). HEALTH readings 에 필수 동봉 —
 * 값만 보내면 BE 가 거부한다. BE 신뢰 게이트(화이트리스트·MANUAL 거부)의 입력.
 */
data class HealthOrigin(
    // 기록 앱 packageName (예: com.sec.android.app.shealth)
    val dataOrigin: String,
    val recordingMethod: RecordingMethod,
    val deviceType: HealthDeviceType,
)

/** Health Connect 읽은 값 1건 (명세 §6.2). 집계·게이트 판정은 BE 가 한다. */
data class HealthReading(
    val metric: HealthMetric,
    val value: Double,
    // km / count / min
    val unit: String,
    // EXERCISE 계열만 채움(예: RUNNING), 그 외 null
    val exerciseType: String?,
    val origin: HealthOrigin,
    // 기록 종료 epoch millis (드레인 TTL·정렬용, 전송 페이로드엔 미포함)
    val at: Long,
)

/** 수면 세그먼트 1건 (명세 §6.2, Sleep/Health Connect SleepSessionRecord). */
data class SleepSegment(
    val startAt: Long,
    val endAt: Long,
    // SLEEPING / AWAKE / DEEP / REM 등 (Health Connect stage, 원문 그대로 보존)
    val status: String,
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

    /**
     * 움직임(명세 §8). [date] 는 readings 가 귀속되는 로컬 날짜(YYYY-MM-DD).
     * 하루치를 매 sync 마다 최신 스냅샷으로 재전송한다(누적 거리/걸음은 BE 가 최신값으로 평가).
     */
    data class Health(
        val date: String,
        val readings: List<HealthReading>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = readings.isEmpty()
    }

    /** 수면(명세 §6.2). 익일 배치라 lag 가 길다(§2.9 maxSignalLagHours≈12h). */
    data class Sleep(
        val segments: List<SleepSegment>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = segments.isEmpty()
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

/** 움직임 수집 대상(명세 §3.2·§8). 어떤 metric 을, (운동 계열이면) 어떤 운동만 읽을지. */
data class HealthTarget(
    val metric: HealthMetric,
    // EXERCISE_DURATION/거리 운동 한정 시 채움(예: RUNNING), null=무관
    val exerciseType: String?,
)

/**
 * 신호 수집 스코프 (명세 §3.2). FE 가 자기 활성 챌린지의 대상 패키지·지오펜스를 알므로
 * 그 범위 신호만 담는다(전체 앱 사용량·상시 위치 무차별 전송 금지).
 */
data class SignalScope(
    val targetPackages: Set<String>,
    // 활성 challengeMemberId(지오펜스 requestId) 집합
    val activeRequestIds: Set<String>,
    // 활성 챌린지가 요구하는 Health Connect 지표(명세 §8). 비면 HEALTH 수집 생략.
    val healthTargets: Set<HealthTarget> = emptySet(),
    // 활성 챌린지에 수면 인증이 있으면 true(명세 §6.2). false 면 SLEEP 수집 생략.
    val sleepRequested: Boolean = false,
)
