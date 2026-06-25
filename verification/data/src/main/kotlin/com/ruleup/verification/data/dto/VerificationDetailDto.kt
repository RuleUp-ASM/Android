package com.ruleup.verification.data.dto

import com.ruleup.network.dto.requireField
import com.ruleup.verification.domain.entity.DailyLog
import com.ruleup.verification.domain.entity.Evidence
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.MethodDetail
import com.ruleup.verification.domain.entity.MethodEvaluation
import com.ruleup.verification.domain.entity.OverallStatus
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.TodayVerification
import com.ruleup.verification.domain.entity.VerificationDetail
import com.ruleup.verification.domain.entity.VerifiedVia
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.3 챌린지 인증 여부 판단 ----------
@Serializable
data class EvidenceDto(
    @SerialName("dwellMinutes")
    val dwellMinutes: Int? = null,
    @SerialName("usageMinutes")
    val usageMinutes: Int? = null,
    @SerialName("firstUnlockAt")
    val firstUnlockAt: String? = null,
    @SerialName("distanceKm")
    val distanceKm: Double? = null,
    @SerialName("steps")
    val steps: Int? = null,
    @SerialName("sleepHours")
    val sleepHours: Double? = null,
    @SerialName("healthOrigin")
    val healthOrigin: String? = null,
)

@Serializable
data class TodayDto(
    @SerialName("isTarget")
    val isTarget: Boolean? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("verifiedAt")
    val verifiedAt: String? = null,
    @SerialName("failureReason")
    val failureReason: String? = null,
    @SerialName("evidence")
    val evidence: EvidenceDto? = null,
    @SerialName("verifiedVia")
    val verifiedVia: String? = null,
    @SerialName("disputeClosesAt")
    val disputeClosesAt: String? = null,
    @SerialName("windowClosesAt")
    val windowClosesAt: String? = null,
)

@Serializable
data class MethodDetailDto(
    @SerialName("insideGeofence")
    val insideGeofence: Boolean? = null,
    @SerialName("dwellMinutes")
    val dwellMinutes: Int? = null,
    @SerialName("usageMinutes")
    val usageMinutes: Int? = null,
    @SerialName("goalMinutes")
    val goalMinutes: Int? = null,
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("metric")
    val metric: String? = null,
    @SerialName("value")
    val value: Double? = null,
    @SerialName("goal")
    val goal: Double? = null,
    @SerialName("dataOrigin")
    val dataOrigin: String? = null,
    @SerialName("recordingMethod")
    val recordingMethod: String? = null,
    @SerialName("bedtime")
    val bedtime: String? = null,
    @SerialName("sleepHours")
    val sleepHours: Double? = null,
)

@Serializable
data class MethodDto(
    @SerialName("method")
    val method: String? = null,
    @SerialName("lastEvaluatedAt")
    val lastEvaluatedAt: String? = null,
    @SerialName("detail")
    val detail: MethodDetailDto? = null,
    @SerialName("supported")
    val supported: Boolean? = null,
)

@Serializable
data class DailyLogDto(
    @SerialName("date")
    val date: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("method")
    val method: String? = null,
    @SerialName("verifiedAt")
    val verifiedAt: String? = null,
)

@Serializable
data class VerificationSummaryDto(
    @SerialName("overallStatus")
    val overallStatus: String? = null,
    @SerialName("progressRate")
    val progressRate: Double? = null,
    @SerialName("successDays")
    val successDays: Int? = null,
    @SerialName("targetDays")
    val targetDays: Int? = null,
    @SerialName("remainingDays")
    val remainingDays: Int? = null,
    @SerialName("today")
    val today: TodayDto? = null,
    @SerialName("methods")
    val methods: List<MethodDto>? = null,
    @SerialName("dailyLogs")
    val dailyLogs: List<DailyLogDto>? = null,
)

@Serializable
data class VerificationDetailResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("verification")
    val verification: VerificationSummaryDto? = null,
)

private fun TodayDto?.toDomain(): TodayVerification =
    TodayVerification(
        isTarget = this?.isTarget ?: false,
        status = TodayStatus.fromValue(this?.status),
        verifiedAt = this?.verifiedAt,
        failureReason = FailureReason.fromValue(this?.failureReason),
        evidence =
            this?.evidence?.let {
                Evidence(
                    dwellMinutes = it.dwellMinutes,
                    usageMinutes = it.usageMinutes,
                    firstUnlockAt = it.firstUnlockAt,
                    distanceKm = it.distanceKm,
                    steps = it.steps,
                    sleepHours = it.sleepHours,
                    healthOrigin = it.healthOrigin,
                )
            },
        verifiedVia = VerifiedVia.fromValue(this?.verifiedVia),
        disputeClosesAt = this?.disputeClosesAt,
        windowClosesAt = this?.windowClosesAt,
    )

private fun MethodDto.toDomain(): MethodEvaluation =
    MethodEvaluation(
        method = method.orEmpty(),
        lastEvaluatedAt = lastEvaluatedAt,
        detail =
            detail?.let {
                MethodDetail(
                    insideGeofence = it.insideGeofence,
                    dwellMinutes = it.dwellMinutes,
                    usageMinutes = it.usageMinutes,
                    goalMinutes = it.goalMinutes,
                    mode = it.mode,
                    metric = it.metric,
                    value = it.value,
                    goal = it.goal,
                    dataOrigin = it.dataOrigin,
                    recordingMethod = it.recordingMethod,
                    bedtime = it.bedtime,
                    sleepHours = it.sleepHours,
                )
            },
        // Android 은 스크린타임 지원이라 기본 true(iOS 대비 응답값 우선).
        supported = supported ?: true,
    )

private fun DailyLogDto.toDomain(): DailyLog =
    DailyLog(
        date = date.requireField("dailyLogs.date"),
        status = TodayStatus.fromValue(status),
        method = method,
        verifiedAt = verifiedAt,
    )

internal fun VerificationDetailResponse.toDomain(): VerificationDetail {
    val v = verification
    return VerificationDetail(
        challengeId = challengeId.requireField("challengeId"),
        title = title.orEmpty(),
        status = status.orEmpty(),
        overallStatus = OverallStatus.fromValue(v?.overallStatus),
        progressRate = v?.progressRate ?: 0.0,
        successDays = v?.successDays ?: 0,
        targetDays = v?.targetDays ?: 0,
        remainingDays = v?.remainingDays ?: 0,
        today = v?.today.toDomain(),
        methods = v?.methods.orEmpty().map { it.toDomain() },
        dailyLogs = v?.dailyLogs.orEmpty().map { it.toDomain() },
    )
}
