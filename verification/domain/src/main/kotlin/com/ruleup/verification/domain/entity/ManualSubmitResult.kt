package com.ruleup.verification.domain.entity

/**
 * 수동 인증 제출 결과 (명세: POST /challenges/{id}/verifications).
 *
 * 제출 즉시 확정되므로 [status] 는 `DONE` 고정이고 잠정 상태가 없다 — 자동 방의 실패 구제는
 * 수동 폴백이 아니라 이의 제기가 담당한다.
 *
 * [verificationId] 는 체크 해제(`DELETE /verifications/{id}`)의 키다. 이 값이 없으면 방금 한 체크를
 * 되돌릴 경로가 없다.
 */
data class ManualSubmitResult(
    val verificationId: String,
    val targetDate: String,
    val status: TodayResultStatus?,
    val streak: VerificationStreak?,
    // 점수 미반영 표시(`MANUAL_NO_SCORE` 고정). 값이 늘면 그때 enum 으로 올린다.
    val scoreNote: String?,
)
