package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeDraft
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeModeration
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ModerationState
import com.ruleup.challenge.domain.entity.ParamEntry
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

/** 기간 `{ start, end }`. 공개 상세만 `remainingDays` 를 덧붙인다. */
@Serializable
data class PeriodDto(
    @SerialName("start")
    val start: String? = null,
    @SerialName("end")
    val end: String? = null,
    @SerialName("remainingDays")
    val remainingDays: Int? = null,
)

internal fun PeriodDto?.toDomain(): ChallengePeriod =
    ChallengePeriod(
        start = this?.start.orEmpty(),
        end = this?.end.orEmpty(),
        remainingDays = this?.remainingDays,
    )

internal fun ChallengePeriod.toDto(): PeriodDto = PeriodDto(start = start, end = end)

/**
 * 패널티 `{ score, groupShare, watcher }`.
 *
 * 응답은 셋 다 오지만 요청에는 `watcher` 만 실린다 — score·groupShare 는 서버가 강제하므로 보내봐야 무시된다.
 */
@Serializable
data class PenaltiesDto(
    @SerialName("score")
    val score: Boolean? = null,
    @SerialName("groupShare")
    val groupShare: Boolean? = null,
    @SerialName("watcher")
    val watcher: Boolean? = null,
)

internal fun PenaltiesDto?.toDomain(): ChallengePenalties =
    ChallengePenalties(
        score = this?.score ?: false,
        groupShare = this?.groupShare ?: false,
        watcher = this?.watcher ?: false,
    )

/** 인증 `{ type, method, detail, requiredPermissions }`. */
@Serializable
data class VerificationDto(
    @SerialName("type")
    val type: String? = null,
    @SerialName("method")
    val method: String? = null,
    // 공개 상세에서만 — 표시 문구
    @SerialName("detail")
    val detail: String? = null,
    @SerialName("requiredPermissions")
    val requiredPermissions: List<String>? = null,
)

/**
 * 미지의 method 는 [VerificationMethod.SELF_CHECK] 로 떨어뜨린다 — 모르는 자동 인증을 처리하는 척하는 것보다
 * 수동으로 보이는 편이 안전하다. type 이 비어 오면 method 로 역추론한다.
 */
internal fun VerificationDto?.toDomain(): VerificationConfig {
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

/** 요청에는 `type`·`method` 만 싣는다 — detail·requiredPermissions 는 서버가 정하는 값이다. */
internal fun VerificationConfig.toDto(): VerificationDto = VerificationDto(type = type.value, method = method.value)

/**
 * 목표값 스펙 `{ key, value, defaultValue, kind, unit, min, max }`.
 *
 * `value`·`defaultValue` 는 서버가 `"06:00"` 처럼 문자열로도, `3` 처럼 숫자로도 보낸다. 어느 쪽이 오든
 * 도메인에서는 문자열 하나로 다루므로 [JsonElement] 로 받아 표현을 보존한 채 펼친다.
 */
@Serializable
data class ParamSpecDto(
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

internal fun ParamSpecDto.toDomain(): ParamSpec =
    ParamSpec(
        key = key.orEmpty(),
        value = value.toParamString(),
        // 기본값이 비어 오면 현재값을 되돌리기 기준으로 쓴다 — 되돌리기가 값을 비우면 안 된다.
        defaultValue = defaultValue?.toParamString() ?: value.toParamString(),
        kind = ParamKind.fromValue(kind),
        unit = unit,
        min = min,
        max = max,
    )

/** 따옴표를 벗긴 원본 표기. 숫자든 문자열이든 사용자가 입력한 그대로를 보존한다. */
private fun JsonElement?.toParamString(): String = this?.jsonPrimitive?.content.orEmpty()

/** 생성·수정 요청의 목표값 `{ key, value }`. */
@Serializable
data class ParamEntryDto(
    @SerialName("key")
    val key: String,
    @SerialName("value")
    val value: String,
)

internal fun ParamEntry.toDto(): ParamEntryDto = ParamEntryDto(key = key, value = value)

/** 항목별 심사 상태 `{ title, description, image }`. */
@Serializable
data class ModerationDto(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("image")
    val image: String? = null,
)

internal fun ModerationDto.toDomain(): ChallengeModeration =
    ChallengeModeration(
        title = ModerationState.fromValue(title),
        description = ModerationState.fromValue(description),
        image = ModerationState.fromValue(image),
    )

/**
 * 초안 본문. `draft` · `by-template` · `clone` 세 API 가 **같은 스키마**로 내려주므로 하나로 공유한다.
 */
@Serializable
data class DraftDto(
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
    val period: PeriodDto? = null,
    @SerialName("weeklyCount")
    val weeklyCount: Int? = null,
    @SerialName("params")
    val params: List<ParamSpecDto>? = null,
    @SerialName("verification")
    val verification: VerificationDto? = null,
    @SerialName("penalties")
    val penalties: PenaltiesDto? = null,
)

internal fun DraftDto.toDomain(): ChallengeDraft =
    ChallengeDraft(
        title = title.orEmpty(),
        description = description.orEmpty(),
        category = Category.fromValue(category.orEmpty()),
        mode = ChallengeMode.fromValue(mode) ?: ChallengeMode.SOLO,
        visibility = visibility?.let(ChallengeVisibility::fromValue),
        rankingVisible = rankingVisible,
        capacity = capacity ?: DEFAULT_CAPACITY,
        minTier = minTier?.let(Tier::fromValue),
        period = period.toDomain(),
        // 명세: 빈도 언급이 없거나 템플릿 진입이면 기본 7(=매일).
        weeklyCount = (weeklyCount ?: DEFAULT_WEEKLY_COUNT).coerceIn(1, 7),
        params = params.orEmpty().map { it.toDomain() },
        verification = verification.toDomain(),
        penalties = penalties.toDomain(),
    )

/** 명세 기본 정원. 서버가 값을 빠뜨렸을 때만 쓰인다. */
private const val DEFAULT_CAPACITY = 50

/** 명세 기본 주간 횟수(7 = 매일). 서버가 값을 빠뜨렸을 때만 쓰인다. */
internal const val DEFAULT_WEEKLY_COUNT = 7
