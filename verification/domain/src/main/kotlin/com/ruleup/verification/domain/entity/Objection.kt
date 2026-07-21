package com.ruleup.verification.domain.entity

/** 이의 제기 유형 (명세 objections). MVP 는 FAILURE 만 지원(ABUSE_PENALTY 는 Phase 2 예약). */
enum class ObjectionType(
    val value: String,
) {
    FAILURE("FAILURE"),
    ;

    companion object {
        fun fromValue(value: String?): ObjectionType? = entries.find { it.value == value }
    }
}

/** 이의 제기 상태 (명세 objections). */
enum class ObjectionStatus(
    val value: String,
) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ;

    companion object {
        fun fromValue(value: String?): ObjectionStatus? = entries.find { it.value == value }
    }
}

/** 이의 제기 결정 (명세 objections/{id}/decision). */
enum class ObjectionDecision(
    val value: String,
) {
    APPROVE("APPROVE"),
    REJECT("REJECT"),
}

/** 확인 대기함 항목 종류 (명세 pending-reviews). */
enum class ReviewKind(
    val value: String,
) {
    FALLBACK("FALLBACK"),
    OBJECTION("OBJECTION"),
    ;

    companion object {
        fun fromValue(value: String?): ReviewKind? = entries.find { it.value == value }
    }
}

/** 이의 제기 제출 결과 (명세 POST objections). */
data class ObjectionTicket(
    val objectionId: String,
    val type: ObjectionType,
    val status: ObjectionStatus,
    // 이의 제기 창 마감 시각(ISO-8601)
    val deadline: String,
)

/** 이의 제기 승인/기각 결과 (명세 POST objections/{id}/decision). */
data class ObjectionDecisionResult(
    val objectionId: String,
    val status: ObjectionStatus,
    val targetDate: String,
    // 처리 결과: 승인=SUCCESS / 기각=FAILED (문자열 그대로)
    val resultStatus: String,
    // 승인 시 OBJECTION
    val verifiedVia: VerifiedVia?,
)

/** 확인 대기함 항목 1개 (명세 pending-reviews items[]). */
data class PendingReviewItem(
    val kind: ReviewKind,
    // 처리 대상 ID (verificationId 또는 objectionId)
    val id: String,
    val userId: String,
    // 익명 챌린지는 마스킹
    val nickname: String,
    val targetDate: String,
    val content: String?,
    val imageUrl: String?,
    // 제출 시각(ISO-8601)
    val submittedAt: String,
    // OBJECTION 이면 이의 제기 창 마감 시각
    val deadline: String?,
)

/** 방장/공동 관리자 확인 대기함 (명세 GET pending-reviews). */
data class PendingReviews(
    val challengeId: String,
    val items: List<PendingReviewItem>,
)
