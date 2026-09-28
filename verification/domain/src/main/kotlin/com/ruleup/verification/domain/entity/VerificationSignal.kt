package com.ruleup.verification.domain.entity

/** 지오펜스 전이 종류. */
enum class GeofenceTransitionType {
    ENTER,
    EXIT,
    DWELL,
}

/** 대상 앱 전후면 전이. */
enum class AppEventType {
    RESUMED,
    PAUSED,
    STOPPED,
}

/** 지오펜스 전이 1건. */
data class GeofenceTransitionEvent(
    // 등록 시 부여한 지오펜스 requestId ("{userId}#{challengeId}#{index}")
    val requestId: String,
    val transition: GeofenceTransitionType,
    // 발생 시각 epoch millis (triggeringLocation.time 우선, 없으면 수신 시각)
    val observedAt: Long,
    // 수신 시점 SystemClock.elapsedRealtime()
    val observedElapsedMillis: Long,
    val accuracy: Float?,
    val isMock: Boolean?,
)

/** 대상 앱 사용 이벤트 1건. */
data class AppUsageEvent(
    val packageName: String,
    val eventType: AppEventType,
    val at: Long,
)

/** 보조 측위 샘플 1건. */
data class LocationPoint(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val isMock: Boolean,
    val at: Long,
)

/** 움직임(HEALTH) 지표. */
enum class HealthMetric {
    DISTANCE,
    STEPS,
    EXERCISE_DURATION,
}

/** 기록 방식. */
enum class RecordingMethod {
    AUTO,
    ACTIVE,
    MANUAL,
    UNKNOWN,
}

/** Health Connect 읽은 값 1건. */
data class HealthReading(
    val recordId: String,
    // 단위는 metric 이 정한다
    val value: Double,
    val startTime: Long,
    val endTime: Long,
    val recordingMethod: RecordingMethod,
    // 기록 앱 packageName (예: com.sec.android.app.shealth)
    val originPackage: String,
)

/** 수면 세션 1건. */
data class SleepSession(
    val recordId: String,
    val start: Long,
    val end: Long,
    val durationMillis: Long,
    val sleepMillis: Long?,
    // 읽기 시점 SystemClock.elapsedRealtime()
    val observedElapsedMillis: Long,
    val recordingMethod: RecordingMethod,
    val originPackage: String,
)

/** sync 페이로드의 신호 단위. */
sealed interface VerificationSignal {
    val isEmpty: Boolean

    /** 이벤트 목록을 앞·뒤 반으로 가른다 (413 재전송용). */
    fun halve(): Pair<VerificationSignal, VerificationSignal?>

    data class GeofenceTransitions(
        val events: List<GeofenceTransitionEvent>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = events.isEmpty()

        override fun halve(): Pair<VerificationSignal, VerificationSignal?> = events.halved(::GeofenceTransitions)
    }

    /** 앱 사용. */
    data class ScreenTime(
        val appEvents: List<AppUsageEvent>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = appEvents.isEmpty()

        override fun halve(): Pair<VerificationSignal, VerificationSignal?> = appEvents.halved(::ScreenTime)
    }

    /** 기상. */
    data class Wake(
        val firstUnlock: Long?,
        val firstScreenOn: Long?,
        val deviceSecure: Boolean,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = firstUnlock == null && firstScreenOn == null

        /** 목록이 아니라 당일 가공값 한 벌이라 가를 수 없다 */
        override fun halve(): Pair<VerificationSignal, VerificationSignal?> = this to null
    }

    data class Locations(
        val points: List<LocationPoint>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = points.isEmpty()

        override fun halve(): Pair<VerificationSignal, VerificationSignal?> = points.halved(::Locations)
    }

    /** 움직임. */
    data class Health(
        val date: String,
        val metric: HealthMetric,
        val readings: List<HealthReading>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = readings.isEmpty()

        override fun halve(): Pair<VerificationSignal, VerificationSignal?> =
            readings.halved { Health(date = date, metric = metric, readings = it) }
    }

    /** 수면. */
    data class Sleep(
        val sessions: List<SleepSession>,
    ) : VerificationSignal {
        override val isEmpty: Boolean get() = sessions.isEmpty()

        override fun halve(): Pair<VerificationSignal, VerificationSignal?> = sessions.halved(::Sleep)
    }
}

/** 30분 배치 단위 신호 묶음. */
data class SignalBatch(
    // ISO datetime
    val collectedAt: String,
    val signals: List<VerificationSignal>,
) {
    val isEmpty: Boolean get() = signals.all { it.isEmpty }

    /** 413 `SYNC_PAYLOAD_TOO_LARGE` 를 받았을 때 쪼갤 두 조각. */
    fun split(): Pair<SignalBatch, SignalBatch>? {
        val halves = signals.map { it.halve() }
        val tail = halves.mapNotNull { it.second }
        if (tail.isEmpty()) return null
        val head = halves.map { it.first }.filterNot { it.isEmpty }
        return SignalBatch(collectedAt, head) to SignalBatch(collectedAt, tail)
    }
}

private fun <T> List<T>.halved(wrap: (List<T>) -> VerificationSignal): Pair<VerificationSignal, VerificationSignal?> {
    if (size <= 1) return wrap(this) to null
    val cut = (size + 1) / 2
    return wrap(take(cut)) to wrap(drop(cut))
}

/** 움직임 수집 대상. */
data class HealthTarget(
    val metric: HealthMetric,
    // EXERCISE_DURATION/거리 운동 한정 시 채움(예: RUNNING), null=무관
    val exerciseType: String?,
)

/** 신호 수집 스코프. */
data class SignalScope(
    val targetPackages: Set<String>,
    // 등록된 지오펜스 requestId 집합
    val activeRequestIds: Set<String>,
    // 활성 챌린지가 요구하는 Health Connect 지표.
    val healthTargets: Set<HealthTarget> = emptySet(),
    // 활성 챌린지에 수면 인증이 있으면 true.
    val sleepRequested: Boolean = false,
    val activeChallengeIds: Set<String>? = null,
)
