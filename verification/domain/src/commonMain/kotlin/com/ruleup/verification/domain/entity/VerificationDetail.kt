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
)

/** 근거 요약 (명세 3.3 evidence). 방식에 따라 일부만 채워진다. */
data class Evidence(
    val dwellMinutes: Int?,
    val usageMinutes: Int?,
    val firstUnlockAt: String?,
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

/** 방식별 상세 (GPS·SCREEN_TIME 이질 필드를 한 타입에 옵셔널로 담는다, 명세 3.3 detail). */
data class MethodDetail(
    // GPS
    val insideGeofence: Boolean?,
    val dwellMinutes: Int?,
    // SCREEN_TIME
    val usageMinutes: Int?,
    val goalMinutes: Int?,
    // MIN / MAX
    val mode: String?,
)

/** 최근 일자별 로그 (명세 3.3 dailyLogs[]). */
data class DailyLog(
    val date: String,
    val status: TodayStatus,
    val method: String?,
    val verifiedAt: String?,
)
