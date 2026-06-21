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

/** 수동 인증 제출 결과 (명세 3.4 response). */
data class ManualSubmitResult(
    val targetDate: String,
    val status: TodayStatus,
    val method: ManualMethod,
    val progressRate: Double,
)
