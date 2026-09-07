package com.ruleup.challenge.domain.entity

/** 방장 위임 요청 상태 (명세 delegation). */
enum class DelegationStatus(
    val value: String,
) {
    PENDING("PENDING"),
    ACCEPTED("ACCEPTED"),
    REJECTED("REJECTED"),
    CANCELED("CANCELED"),
    EXPIRED("EXPIRED"),
    ;

    companion object {
        fun fromValue(value: String?): DelegationStatus? = entries.find { it.value == value }
    }
}

/** 위임 요청 응답 액션 (명세 PATCH delegation/{id}). */
enum class DelegationAction(
    val value: String,
) {
    ACCEPT("ACCEPT"),
    REJECT("REJECT"),
    CANCEL("CANCEL"),
}

/** 방장 위임 요청 생성 결과 (명세 POST delegation). 7일 후 자동 만료. */
data class DelegationTicket(
    val delegationId: String,
    val status: DelegationStatus,
    // 만료 시각(요청 +7일, ISO-8601)
    val expiresAt: String,
)

/**
 * 위임 요청 응답 결과 (명세 PATCH delegation/{id}). ACCEPTED 시 role swap 이 일어나며
 * [newOwnerUserId] 로 새 방장을 알려준다(그 외 null).
 */
data class DelegationResolution(
    val status: DelegationStatus,
    val newOwnerUserId: String?,
)
