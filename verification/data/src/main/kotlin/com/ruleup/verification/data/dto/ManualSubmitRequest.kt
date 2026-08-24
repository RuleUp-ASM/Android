package com.ruleup.verification.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 수동 인증 제출 요청 (명세: POST /challenges/{id}/verifications) ----------

/**
 * 둘 다 선택 항목이다. [targetDate] 를 비우면 서버가 오늘로 잡고, [note] 는 기록용이라 검증하지 않는다.
 * 구 계약의 `method`·`imageUrl`·`asFallback` 은 폐기됐다 — 수동 인증은 자체 체크 하나뿐이고,
 * 자동 방의 실패 구제는 이의 제기가 담당한다.
 */
@Serializable
data class ManualSubmitRequest(
    @SerialName("targetDate")
    val targetDate: String? = null,
    @SerialName("note")
    val note: String? = null,
)
