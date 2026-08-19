package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.AppealChance
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.UnacknowledgedResult
import com.ruleup.verification.domain.entity.VerificationStreak
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 오늘 인증 결과 (GET /challenges/{id}/verifications/today) ----------

@Serializable
data class StreakResponse(
    @SerialName("before")
    val before: Int? = null,
    @SerialName("after")
    val after: Int? = null,
)

@Serializable
data class UnacknowledgedResultResponse(
    @SerialName("verificationId")
    val verificationId: String? = null,
    @SerialName("result")
    val result: String? = null,
)

@Serializable
data class AppealChanceResponse(
    // 실패 확정 +7일
    @SerialName("eligibleUntil")
    val eligibleUntil: String? = null,
    @SerialName("remainingThisMonth")
    val remainingThisMonth: Int? = null,
    @SerialName("eligible")
    val eligible: Boolean? = null,
)

@Serializable
data class TodayResultResponse(
    @SerialName("date")
    val date: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("window")
    val window: String? = null,
    @SerialName("confirmedAt")
    val confirmedAt: String? = null,
    @SerialName("failureReason")
    val failureReason: String? = null,
    @SerialName("streak")
    val streak: StreakResponse? = null,
    @SerialName("unacknowledgedResult")
    val unacknowledgedResult: UnacknowledgedResultResponse? = null,
    // FAILED 일 때만 내려온다
    @SerialName("appeal")
    val appeal: AppealChanceResponse? = null,
)

internal fun TodayResultResponse.toDomain(): TodayResult =
    TodayResult(
        date = date.orEmpty(),
        // 미인식 상태는 null — 모르는 값을 성공·실패로 접지 않는다.
        status = TodayResultStatus.fromValue(status),
        window = window,
        confirmedAt = confirmedAt,
        failureReason = FailureReason.fromValue(failureReason),
        streak =
            streak?.let {
                VerificationStreak(before = it.before ?: 0, after = it.after ?: 0)
            },
        // verificationId 가 없으면 ack 를 부를 수 없어 모달을 띄울 의미가 없다.
        unacknowledged =
            unacknowledgedResult?.verificationId?.let {
                UnacknowledgedResult(verificationId = it, result = unacknowledgedResult.result.orEmpty())
            },
        appeal =
            appeal?.let {
                AppealChance(
                    eligibleUntil = it.eligibleUntil,
                    remainingThisMonth = it.remainingThisMonth ?: 0,
                    // eligible 이 없으면 남은 횟수로 판단한다 — 둘은 같은 사실의 다른 표현이다.
                    eligible = it.eligible ?: ((it.remainingThisMonth ?: 0) > 0),
                )
            },
    )
