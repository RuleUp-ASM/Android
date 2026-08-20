package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.AppealReceipt
import com.ruleup.verification.domain.entity.AppealRestored
import com.ruleup.verification.domain.entity.AppealTrack
import com.ruleup.verification.domain.entity.TodayResultStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 인증 이의 제기 (POST /verifications/{verificationId}/appeals) ----------

@Serializable
data class SubmitAppealRequest(
    // 필수, 10자 이상 — 미달이면 400 INVALID_REASON 이고 접수·이력 어디에도 남지 않는다
    @SerialName("reason")
    val reason: String,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)

@Serializable
data class AppealRestoredResponse(
    @SerialName("verification")
    val verification: String? = null,
    @SerialName("streak")
    val streak: Int? = null,
    @SerialName("scoreDelta")
    val scoreDelta: Int? = null,
)

/** `result` 는 `ACCEPTED` 고정이라 도메인으로 올리지 않는다 — 접수됐다는 것이 곧 인용이다. */
@Serializable
data class AppealResponse(
    @SerialName("appealId")
    val appealId: String? = null,
    @SerialName("track")
    val track: String? = null,
    @SerialName("restored")
    val restored: AppealRestoredResponse? = null,
)

internal fun AppealResponse.toDomain(): AppealReceipt =
    AppealReceipt(
        appealId = appealId.orEmpty(),
        track = AppealTrack.fromValue(track),
        restored =
            restored?.let {
                AppealRestored(
                    verification = TodayResultStatus.fromValue(it.verification),
                    streak = it.streak,
                    scoreDelta = it.scoreDelta,
                )
            },
    )
