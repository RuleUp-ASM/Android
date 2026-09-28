package com.ruleup.verification.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 수동 인증 제출 요청

/** 둘 다 선택 항목이다. */
@Serializable
data class ManualSubmitRequest(
    @SerialName("targetDate")
    val targetDate: String? = null,
    @SerialName("note")
    val note: String? = null,
)
