package com.ruleup.verification.domain.entity

/**
 * 챌린지 인증 여부 판단 (명세 3.3). 성공 화면·실패 화면을 한 응답으로 렌더한다.
 */
data class VerificationDetail(
    val challengeId: String,
    val title: String,
    val status: String,
    val overallStatus: OverallStatus,
    val progressRate: Double,
    val successDays: Int,
    val targetDays: Int,
    val remainingDays: Int,
    val today: TodayVerification,
    val methods: List<MethodEvaluation>,
    val dailyLogs: List<DailyLog>,
)

/** 오늘 인증 상태 + 근거 (명세 3.3 verification.today). */
data class TodayVerification(
    val isTarget: Boolean,
    val status: TodayStatus,
    val verifiedAt: String?,
    val failureReason: FailureReason?,
    val evidence: Evidence?,
    // 인증 경로(명세 §9.2). MANUAL_FALLBACK 이면 [disputeClosesAt] 까지 잠정 성공.
    val verifiedVia: VerifiedVia?,
    val disputeClosesAt: String?,
    // 제약형·시간창 "언제 확정"(명세 3.3 windowClosesAt).
    val windowClosesAt: String?,
)

/** 근거 요약 (명세 3.3 evidence). 방식에 따라 일부만 채워진다. */
data class Evidence(
    val dwellMinutes: Int?,
    val usageMinutes: Int?,
    val firstUnlockAt: String?,
    // 움직임(명세 §8) 근거.
    val distanceKm: Double?,
    val steps: Int?,
    // 수면(명세 §6.2) 근거.
    val sleepHours: Double?,
    // 신뢰 게이트 입력 기록 앱(명세 §8.2).
    val healthOrigin: String?,
)

/**
 * 방식별 마지막 평가 (명세 3.3 methods[]).
 * [supported]=false 는 iOS 스크린타임 등 플랫폼 미지원. Android 에선 거의 항상 true.
 */
data class MethodEvaluation(
    // GPS / SCREEN_TIME ...
    val method: String,
    val lastEvaluatedAt: String?,
    val detail: MethodDetail?,
    val supported: Boolean,
)

/** 방식별 상세 (GPS·SCREEN_TIME·HEALTH·SLEEP 이질 필드를 한 타입에 옵셔널로 담는다, 명세 3.3 detail). */
data class MethodDetail(
    // GPS
    val insideGeofence: Boolean?,
    val dwellMinutes: Int?,
    // SCREEN_TIME
    val usageMinutes: Int?,
    val goalMinutes: Int?,
    // MIN / MAX
    val mode: String?,
    // HEALTH(명세 §8) — metric/value/goal + 신뢰 메타데이터.
    val metric: String?,
    val value: Double?,
    val goal: Double?,
    val dataOrigin: String?,
    val recordingMethod: String?,
    // SLEEP(명세 §6.2).
    val bedtime: String?,
    val sleepHours: Double?,
)

/** 최근 일자별 로그 (명세 3.3 dailyLogs[]). */
data class DailyLog(
    val date: String,
    val status: TodayStatus,
    val method: String?,
    val verifiedAt: String?,
)
