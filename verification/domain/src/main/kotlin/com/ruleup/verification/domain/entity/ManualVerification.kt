package com.ruleup.verification.domain.entity

/**
 * 수동 인증 방식 (명세 3.4). MVP 는 [SELF_CHECK] 우선, [PHOTO] 는 포렌식 없는 단순 업로드라
 * 그룹 리뷰 붙기 전까지 비강조(명세 §6.5).
 */
enum class ManualMethod(
    val value: String,
) {
    PHOTO("PHOTO"),
    SELF_CHECK("SELF_CHECK"),
    ;

    companion object {
        fun fromValue(value: String?): ManualMethod? = entries.find { it.value == value }
    }
}

/**
 * 인증 경로 (명세 §9.2). [MANUAL_FALLBACK] 은 잠정 성공 — 이의 윈도우 후 확정.
 */
enum class VerifiedVia(
    val value: String,
) {
    AUTO("AUTO"),
    MANUAL("MANUAL"),
    MANUAL_FALLBACK("MANUAL_FALLBACK"),
    ;

    companion object {
        fun fromValue(value: String?): VerifiedVia? = entries.find { it.value == value }
    }
}

/**
 * 수동 인증 제출 결과 (명세 3.4·§9.2 response).
 * [verifiedVia]=MANUAL_FALLBACK 이면 [disputeClosesAt] 까지 잠정 성공(침묵=동의로 확정).
 */
data class ManualSubmitResult(
    val targetDate: String,
    val status: TodayStatus,
    val method: ManualMethod,
    val progressRate: Double,
    val verifiedVia: VerifiedVia,
    // 예비 폴백 이의 윈도우 종료(ISO). 정규 수동(MANUAL)이면 null.
    val disputeClosesAt: String?,
)
