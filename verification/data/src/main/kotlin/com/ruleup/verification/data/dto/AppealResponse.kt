package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.AppealHistoryItem
import com.ruleup.verification.domain.entity.AppealReceipt
import com.ruleup.verification.domain.entity.AppealRestored
import com.ruleup.verification.domain.entity.AppealTrack
import com.ruleup.verification.domain.entity.TodayResultStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppealRestoredResponse(
    @SerialName("verification")
    val verification: String? = null,
    @SerialName("streak")
    val streak: Int? = null,
    @SerialName("scoreDelta")
    val scoreDelta: Int? = null,
)

/** `result` 는 `ACCEPTED` 고정이라 도메인으로 올리지 않는다 */
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

// 증빙 사진 업로드 (POST /appeals/images)

@Serializable
data class AppealImageResponse(
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)

// 이의 이력 (GET /users/me/appeals)

@Serializable
data class AppealHistoryItemResponse(
    @SerialName("appealId")
    val appealId: String? = null,
    @SerialName("date")
    val date: String? = null,
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("routineTitle")
    val routineTitle: String? = null,
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("track")
    val track: String? = null,
)

/** 응답 예시에는 `credits`·`resetAt`·`creditUsed` 도 있지만 읽지 않는다 */
@Serializable
data class MyAppealsResponse(
    @SerialName("history")
    val history: List<AppealHistoryItemResponse>? = null,
)

internal fun MyAppealsResponse.toDomain(): List<AppealHistoryItem> =
    history.orEmpty().mapNotNull { item ->
        // 식별자가 없는 행은 버린다
        val id = item.appealId ?: return@mapNotNull null
        AppealHistoryItem(
            appealId = id,
            date = item.date.orEmpty(),
            challengeId = item.challengeId.orEmpty(),
            routineTitle = item.routineTitle.orEmpty(),
            reason = item.reason.orEmpty(),
            track = AppealTrack.fromValue(item.track),
        )
    }
