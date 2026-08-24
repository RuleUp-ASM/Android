package com.ruleup.verification.data.dto

import com.ruleup.network.dto.requireField
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.VerificationStreak
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 수동 인증 제출 응답 (명세: POST /challenges/{id}/verifications) ----------
@Serializable
data class ManualSubmitResponse(
    @SerialName("verificationId")
    val verificationId: String? = null,
    @SerialName("targetDate")
    val targetDate: String? = null,
    // DONE 고정 — 수동 제출은 즉시 확정된다
    @SerialName("status")
    val status: String? = null,
    @SerialName("streak")
    val streak: StreakResponse? = null,
    // MANUAL_NO_SCORE 고정 — 점수는 안 붙지만 성공률·랭킹·통계에는 들어간다
    @SerialName("scoreNote")
    val scoreNote: String? = null,
)

internal fun ManualSubmitResponse.toDomain(): ManualSubmitResult =
    ManualSubmitResult(
        // 이 값이 없으면 방금 한 체크를 되돌릴 경로가 없다.
        verificationId = verificationId.requireField("verificationId"),
        targetDate = targetDate.requireField("targetDate"),
        // 미인식 상태는 null — 모르는 값을 완료로 접으면 안 된 인증이 된 것처럼 보인다.
        status = TodayResultStatus.fromValue(status),
        streak = streak?.let { VerificationStreak(before = it.before ?: 0, after = it.after ?: 0) },
        scoreNote = scoreNote,
    )

// ---------- 수동 인증 취소 (DELETE /verifications/{verificationId}) ----------

@Serializable
data class CancelManualResponse(
    @SerialName("canceled")
    val canceled: Boolean? = null,
)
