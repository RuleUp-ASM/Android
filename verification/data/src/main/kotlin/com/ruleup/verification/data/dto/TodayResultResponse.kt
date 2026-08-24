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
    // 실패 확정 후 1일 (인증 정책 §5.2)
    @SerialName("eligibleUntil")
    val eligibleUntil: String? = null,
    @SerialName("eligible")
    val eligible: Boolean? = null,
)

@Serializable
data class TodayResultResponse(
    @SerialName("date")
    val date: String? = null,
    // 이의 접수 대상 인증 건 ID. 명세에 아직 없어 서버가 안 줄 수 있고, 그때는
    // unacknowledgedResult 쪽 ID 로 폴백한다(BE 에 최상위 노출 요청 중).
    @SerialName("verificationId")
    val verificationId: String? = null,
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
        verificationId = verificationId ?: unacknowledgedResult?.verificationId,
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
                    // 한도가 없어졌으므로 eligible 이 없으면 기한만 남은 조건이다 — 낼 수 있다고 본다.
                    eligible = it.eligible ?: true,
                )
            },
    )

// ---------- 판정 결과 확인 (POST /verifications/{verificationId}/ack) ----------

/** 멱등 응답 — 이미 확인한 건을 다시 불러도 `true` 로 온다. */
@Serializable
data class AcknowledgeResponse(
    @SerialName("acknowledged")
    val acknowledged: Boolean? = null,
)
