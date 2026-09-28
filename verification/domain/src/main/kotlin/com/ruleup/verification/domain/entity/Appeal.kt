package com.ruleup.verification.domain.entity

/** 인증 이의 제기 규칙. */
object AppealPolicy {
    // 사유 최소 길이.
    const val MIN_REASON_LENGTH = 10
}

/** 이의 처리 트랙. */
enum class AppealTrack(
    val value: String,
) {
    A("A"),
    B("B"),
    ;

    companion object {
        fun fromValue(value: String?): AppealTrack? = entries.find { it.value == value }
    }
}

/** 이의 인용으로 되돌려진 기록. */
data class AppealRestored(
    val verification: TodayResultStatus?,
    val streak: Int?,
    val scoreDelta: Int?,
)

/** 이의 접수 결과. */
data class AppealReceipt(
    val appealId: String,
    val track: AppealTrack?,
    val restored: AppealRestored?,
)

/** 이의 제기 이력 1건. */
data class AppealHistoryItem(
    val appealId: String,
    // 신청일 (YYYY-MM-DD)
    val date: String,
    val challengeId: String,
    val routineTitle: String,
    val reason: String,
    val track: AppealTrack?,
)
