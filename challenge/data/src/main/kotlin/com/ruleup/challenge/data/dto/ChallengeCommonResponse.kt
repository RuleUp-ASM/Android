package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeDraft
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeModeration
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ModerationState
import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

/** 기간 `{ start, end }`. */
@Serializable
data class PeriodResponse(
    @SerialName("start")
    val start: String? = null,
    @SerialName("end")
    val end: String? = null,
    @SerialName("remainingDays")
    val remainingDays: Int? = null,
)

internal fun PeriodResponse?.toDomain(): ChallengePeriod =
    ChallengePeriod(
        start = this?.start.orEmpty(),
        end = this?.end.orEmpty(),
        remainingDays = this?.remainingDays,
    )

/** 패널티 `{ score, groupShare, watcher }`. */
@Serializable
data class PenaltiesResponse(
    @SerialName("score")
    val score: Boolean? = null,
    @SerialName("groupShare")
    val groupShare: Boolean? = null,
    @SerialName("watcher")
    val watcher: Boolean? = null,
)

internal fun PenaltiesResponse?.toDomain(): ChallengePenalties =
    ChallengePenalties(
        score = this?.score ?: false,
        groupShare = this?.groupShare ?: false,
        watcher = this?.watcher ?: false,
    )

/** 인증 `{ type, method, detail, requiredPermissions }`. */
@Serializable
data class VerificationResponse(
    @SerialName("type")
    val type: String? = null,
    @SerialName("method")
    val method: String? = null,
    // 공개 상세에서만
    @SerialName("detail")
    val detail: String? = null,
    @SerialName("requiredPermissions")
    val requiredPermissions: List<String>? = null,
)

/** 미지의 method 는 [VerificationMethod.SELF_CHECK] 로 떨어뜨린다 */
internal fun VerificationResponse?.toDomain(): VerificationConfig {
    val method = VerificationMethod.fromValue(this?.method) ?: VerificationMethod.SELF_CHECK
    return VerificationConfig(
        type =
            VerificationType.fromValue(this?.type)
                ?: if (method == VerificationMethod.SELF_CHECK) VerificationType.MANUAL else VerificationType.AUTO,
        method = method,
        detail = this?.detail,
        requiredPermissions = this?.requiredPermissions.orEmpty(),
    )
}

/** 목표값 스펙 `{ key, value, defaultValue, kind, unit, min, max }`. */
@Serializable
data class ParamSpecResponse(
    @SerialName("key")
    val key: String? = null,
    @SerialName("value")
    val value: JsonElement? = null,
    @SerialName("defaultValue")
    val defaultValue: JsonElement? = null,
    @SerialName("kind")
    val kind: String? = null,
    @SerialName("unit")
    val unit: String? = null,
    @SerialName("min")
    val min: Double? = null,
    @SerialName("max")
    val max: Double? = null,
)

internal fun ParamSpecResponse.toDomain(): ParamSpec =
    ParamSpec(
        key = key.orEmpty(),
        value = value.toParamString(),
        // 기본값이 비어 오면 현재값을 되돌리기 기준으로 쓴다
        defaultValue = defaultValue?.toParamString() ?: value.toParamString(),
        kind = ParamKind.fromValue(kind),
        unit = unit,
        min = min,
        max = max,
    )

/** 따옴표를 벗긴 원본 표기. */
private fun JsonElement?.toParamString(): String = this?.jsonPrimitive?.content.orEmpty()

/** 항목별 심사 상태 `{ title, description, image }`. */
@Serializable
data class ModerationResponse(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("image")
    val image: String? = null,
)

internal fun ModerationResponse.toDomain(): ChallengeModeration =
    ChallengeModeration(
        title = moderationState(title),
        description = moderationState(description),
        image = moderationState(image),
    )

/** 심사 상태 문자열 → 도메인. */
private fun moderationState(value: String?): ModerationState =
    when (value) {
        "EXEMPT", "APPROVED" -> ModerationState.APPROVED
        "IN_REVIEW" -> ModerationState.IN_REVIEW
        "REJECTED" -> ModerationState.REJECTED
        // 이미지 미등록("NONE")과 아직 모르는 값을 같이 흡수한다.
        else -> ModerationState.NONE
    }

/** 초안 본문. */
@Serializable
data class DraftContentResponse(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("visibility")
    val visibility: String? = null,
    @SerialName("rankingVisible")
    val rankingVisible: Boolean? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("minTier")
    val minTier: String? = null,
    @SerialName("period")
    val period: PeriodResponse? = null,
    @SerialName("weeklyCount")
    val weeklyCount: Int? = null,
    @SerialName("params")
    val params: List<ParamSpecResponse>? = null,
    @SerialName("verification")
    val verification: VerificationResponse? = null,
    @SerialName("penalties")
    val penalties: PenaltiesResponse? = null,
)

internal fun DraftContentResponse.toDomain(): ChallengeDraft =
    ChallengeDraft(
        title = title.orEmpty(),
        category = Category.fromValue(category.orEmpty()),
        imageUrl = null,
        description = description.orEmpty(),
        mode = ChallengeMode.fromValue(mode) ?: ChallengeMode.SOLO,
        visibility = visibility?.let(ChallengeVisibility::fromValue),
        rankingVisible = rankingVisible,
        // null 은 무제한이다
        capacity = capacity,
        minTier = minTier?.let(Tier::fromValue),
        period = period.toDomain(),
        // 명세: 빈도 언급이 없거나 템플릿 진입이면 기본 7(=매일).
        weeklyCount = (weeklyCount ?: DEFAULT_WEEKLY_COUNT).coerceIn(1, 7),
        params = params.orEmpty().map { it.toDomain() },
        verification = verification.toDomain(),
        penalties = penalties.toDomain(),
    )

/** 명세 기본 주간 횟수(7 = 매일). */
internal const val DEFAULT_WEEKLY_COUNT = 7
