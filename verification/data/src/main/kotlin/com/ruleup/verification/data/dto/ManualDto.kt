package com.ruleup.verification.data.dto

import com.ruleup.network.dto.requireField
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.VerifiedVia
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.4 수동 인증 제출 (명세 §9) ----------
@Serializable
data class ManualSubmitRequest(
    @SerialName("method")
    val method: String? = null,
    @SerialName("targetDate")
    val targetDate: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    // true 면 예비 폴백 규칙(주1회·잠정성공·이의윈도우) 적용(명세 §9.2).
    @SerialName("asFallback")
    val asFallback: Boolean = false,
)

@Serializable
data class ManualSubmitResponse(
    @SerialName("targetDate")
    val targetDate: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("method")
    val method: String? = null,
    @SerialName("progressRate")
    val progressRate: Double? = null,
    @SerialName("verifiedVia")
    val verifiedVia: String? = null,
    @SerialName("disputeClosesAt")
    val disputeClosesAt: String? = null,
)

internal fun ManualSubmitResponse.toDomain(): ManualSubmitResult =
    ManualSubmitResult(
        targetDate = targetDate.requireField("targetDate"),
        status = TodayStatus.fromValue(status),
        method = ManualMethod.fromValue(method) ?: ManualMethod.SELF_CHECK,
        progressRate = progressRate ?: 0.0,
        // 미인식/누락이면 정규 수동(MANUAL)으로 보수적 처리.
        verifiedVia = VerifiedVia.fromValue(verifiedVia) ?: VerifiedVia.MANUAL,
        disputeClosesAt = disputeClosesAt,
    )
