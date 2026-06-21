package com.ruleup.verification.data.dto

import com.ruleup.network.dto.requireField
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.TodayStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.4 수동 인증 제출 ----------
@Serializable
data class ManualSubmitRequest(
    @SerialName("method")
    val method: String? = null,
    @SerialName("targetDate")
    val targetDate: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
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
)

internal fun ManualSubmitResponse.toDomain(): ManualSubmitResult =
    ManualSubmitResult(
        targetDate = targetDate.requireField("targetDate"),
        status = TodayStatus.fromValue(status),
        method = ManualMethod.fromValue(method) ?: ManualMethod.SELF_CHECK,
        progressRate = progressRate ?: 0.0,
    )
