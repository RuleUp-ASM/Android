package com.ruleup.verification.domain.entity

/** 수동 인증 제출 결과. */
data class ManualSubmitResult(
    val verificationId: String,
    val targetDate: String,
    val status: TodayResultStatus?,
    val streak: VerificationStreak?,
    // 점수 미반영 표시(`MANUAL_NO_SCORE` 고정).
    val scoreNote: String?,
)

/** 수동 인증 메모 제한. */
object ManualNoteLimits {
    const val MAX_LENGTH = 200
}
