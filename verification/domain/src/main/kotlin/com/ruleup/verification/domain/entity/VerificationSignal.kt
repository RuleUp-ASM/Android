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

/** 화면/잠금해제 이벤트 종류 (WAKE 판정용, 전송 스펙 §4). 로컬 버퍼의 분류 키로만 쓴다. */
enum class ScreenEventType {
    UNLOCK,
    SCREEN_ON,
}

/**
 * 지오펜스 전이 1건 (전송 스펙 §1). **좌표는 담지 않는다** — 계약에 lat/lng 가 없고, 좌표가 나가는
 * 유일한 통로는 3순위 좌표 가중 체류 챌린지에만 붙는 [LocationPoint] 다.
 *
 * [observedElapsedMillis] 는 수신 시점 monotonic 시각이다. 같은 부팅 세션 안에서 [observedAt]
 * 간격과 대조해 시각 조작을 잡는 입력이라(전송 스펙 §6.4) 벽시계와 함께 반드시 실어 보낸다.
 *
 * [accuracy]·[isMock] 은 OS 가 전이에 위치를 안 실어 보내면 **null** 이다. 기본값으로 접으면
 * 서버가 정확도 0m·"mock 아님"이라는 없던 사실로 읽어 판정이 오염된다.
 */
data class GeofenceTransitionEvent(
    // 등록 시 부여한 지오펜스 requestId ("{userId}#{challengeId}#{index}") — 와이어의 anchorId
    val requestId: String,
    val transition: GeofenceTransitionType,
    // 발생 시각 epoch millis (triggeringLocation.time 우선, 없으면 수신 시각)
    val observedAt: Long,
    // 수신 시점 SystemClock.elapsedRealtime()
    val observedElapsedMillis: Long,
    val accuracy: Float?,
    val isMock: Boolean?,
)

/** 대상 앱 사용 이벤트 1건(시퀀스 그대로 전송, 누적 foregroundSec 금지, 명세 §2.2). */
data class AppUsageEvent(
    val packageName: String,
    val eventType: AppEventType,
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

/**
 * Health Connect 읽은 값 1건 (전송 스펙 §2). 집계·화이트리스트 게이트·MANUAL 거부는 전부 서버가
 * 하고, 클라는 그 판단에 필요한 입력을 빠짐없이 올리는 것까지만 책임진다.
 *
 * [recordId]·[recordingMethod]·[originPackage] 는 **필수 동봉**이다 — 값만 보내면 서버가 거부한다.
 * [recordId] 는 하루치를 매 sync 재전송해도 중복이 안 쌓이는 유일한 근거다.
 *
 * [startTime]·[endTime] 을 함께 보내는 이유는 인증 창이 하루보다 좁은 챌린지가 있어서다 —
 * 날짜만으로는 어느 창에 귀속되는지 정해지지 않는다.
 */
data class HealthReading(
    val recordId: String,
    // 단위는 metric 이 정한다 — DISTANCE=km · STEPS=count · EXERCISE_DURATION=분
    val value: Double,
    val startTime: Long,
    val endTime: Long,
    val recordingMethod: RecordingMethod,
    // 기록 앱 packageName (예: com.sec.android.app.shealth)
    val originPackage: String,
)

/**
 * 수면 세션 1건 (전송 스펙 §5). **stage 로 쪼개지 않고 세션 단위**로 보낸다.
 *
 * [sleepMillis] 는 AWAKE·AWAKE_IN_BED·OUT_OF_BED 를 제외한 stage 합이다. writer 가 stage 를
 * 주지 않으면 null 이고, 그때는 서버가 [durationMillis] 로 대체한다 — 0 으로 접으면 "안 잤다"가 된다.
 */
data class SleepSession(
    val recordId: String,
    val start: Long,
    val end: Long,
    val durationMillis: Long,
    val sleepMillis: Long?,
    // 읽기 시점 SystemClock.elapsedRealtime() — 시각 조작 교차검증(전송 스펙 §6.4)
    val observedElapsedMillis: Long,
    val recordingMethod: RecordingMethod,
    val originPackage: String,
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

    /** 앱 사용(전송 스펙 §3). 페어링·합산은 서버가 하므로 시퀀스를 그대로 보낸다. */
    data class ScreenTime(
        val appEvents: List<AppUsageEvent>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = appEvents.isEmpty()
    }

    /**
     * 기상(전송 스펙 §4). 화면 이벤트 시퀀스가 아니라 **당일 첫 시각만 가공해** 올린다 —
     * 기상 판정은 "첫 잠금해제가 목표 시각 안이었나" 하나만 묻기 때문이다.
     *
     * [deviceSecure] 가 false 면 잠금을 걸지 않은 기기라 [firstUnlock] 이 영영 나오지 않는다.
     * 서버가 [firstScreenOn] 폴백을 쓸지 가르는 입력이라 값이 없어도 함께 보낸다.
     */
    data class Wake(
        val firstUnlock: Long?,
        val firstScreenOn: Long?,
        val deviceSecure: Boolean,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = firstUnlock == null && firstScreenOn == null
    }

    data class Locations(
        val points: List<LocationPoint>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = points.isEmpty()
    }

    /**
     * 움직임(전송 스펙 §2). [metric] 은 **신호 레벨**이라 reading 마다 반복하지 않는다.
     * [date] 는 readings 가 귀속되는 로컬 날짜(YYYY-MM-DD). 하루치를 매 sync 마다 최신 스냅샷으로
     * 재전송하고, 중복은 서버가 `recordId` 로 걸러낸다.
     */
    data class Health(
        val date: String,
        val metric: HealthMetric,
        val readings: List<HealthReading>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = readings.isEmpty()
    }

    /** 수면(전송 스펙 §5). 세션은 깬 뒤 한 번에 기록돼 약 12시간 늦게 도착한다. */
    data class Sleep(
        val sessions: List<SleepSession>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = sessions.isEmpty()
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
    // 등록된 지오펜스 requestId 집합
    val activeRequestIds: Set<String>,
    // 활성 챌린지가 요구하는 Health Connect 지표(명세 §8). 비면 HEALTH 수집 생략.
    val healthTargets: Set<HealthTarget> = emptySet(),
    // 활성 챌린지에 수면 인증이 있으면 true(명세 §6.2). false 면 SLEEP 수집 생략.
    val sleepRequested: Boolean = false,
)
