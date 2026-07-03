package com.ruleup.verification.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.4 수동 인증 제출 요청 (명세 §9) ----------
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
