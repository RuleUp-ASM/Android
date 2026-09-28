package com.ruleup.report.data.dto

import com.ruleup.report.domain.entity.ReportTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 신고 접수 요청. */
@Serializable
data class ReportRequest(
    @SerialName("targetType")
    val targetType: String,
    @SerialName("targetUserId")
    val targetUserId: String? = null,
    @SerialName("targetChallengeId")
    val targetChallengeId: String? = null,
    @SerialName("contextType")
    val contextType: String,
    @SerialName("reason")
    val reason: String,
)

// 와이어 값.
private const val TARGET_USER = "USER"
private const val TARGET_CHALLENGE = "CHALLENGE"

internal fun ReportTarget.toRequest(): ReportRequest =
    when (this) {
        is ReportTarget.User ->
            ReportRequest(
                targetType = TARGET_USER,
                targetUserId = userId,
                // 프로필에서 온 신고면 null 이라 직렬화에서 빠진다.
                targetChallengeId = challengeId,
                contextType = context.value,
                reason = reason.value,
            )

        is ReportTarget.Challenge ->
            ReportRequest(
                targetType = TARGET_CHALLENGE,
                targetChallengeId = challengeId,
                contextType = context.value,
                reason = reason.value,
            )
    }
