package com.ruleup.report.data.dto

import com.ruleup.report.domain.entity.ReportTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 신고 접수 요청(명세 POST /reports).
 *
 * 자유 텍스트 필드는 없다 — 사유 선택만 받는다. `contextId` 도 넣지 않는다: 공지·댓글 신고
 * 전용인데 그 기능이 Phase 2 라 지금은 채울 값이 없다.
 */
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

// 와이어 값. domain 은 sealed 갈래로 대상을 구분하므로 이 문자열은 data 안에만 있으면 된다.
private const val TARGET_USER = "USER"
private const val TARGET_CHALLENGE = "CHALLENGE"

internal fun ReportTarget.toRequest(): ReportRequest =
    when (this) {
        is ReportTarget.User ->
            ReportRequest(
                targetType = TARGET_USER,
                targetUserId = userId,
                // 프로필에서 온 신고면 null 이라 직렬화에서 빠진다. 서버도 그때만 생략을 허용한다.
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
