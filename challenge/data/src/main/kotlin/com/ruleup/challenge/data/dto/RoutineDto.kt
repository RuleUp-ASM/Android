package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.ParamValue
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.entity.SignalSource
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationOption
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.WearableRequirement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/** 추천 options[] 한 건. */
@Serializable
data class VerificationOptionDto(
    @SerialName("method")
    val method: String? = null,
    @SerialName("recommended")
    val recommended: Boolean? = null,
    @SerialName("verificationType")
    val verificationType: String? = null,
    @SerialName("signalSource")
    val signalSource: String? = null,
    @SerialName("wearableRequirement")
    val wearableRequirement: String? = null,
    @SerialName("externalService")
    val externalService: String? = null,
    @SerialName("requiredPermissions")
    val requiredPermissions: List<String>? = null,
)

internal fun VerificationOptionDto.toDomain(): VerificationOption =
    VerificationOption(
        method = SelectedMethod.fromValue(method) ?: SelectedMethod.MANUAL,
        recommended = recommended ?: false,
        verificationType = VerificationType.fromValue(verificationType) ?: VerificationType.MANUAL,
        signalSource = SignalSource.fromValue(signalSource),
        wearableRequirement = WearableRequirement.fromValue(wearableRequirement),
        externalService = externalService,
        requiredPermissions = requiredPermissions.orEmpty(),
    )

/** 추천 params[] 한 건. value/defaultValue 는 Number|String 이라 JsonElement 로 받는다. */
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

internal fun ParamSpecDto.toDomain(): ParamSpec {
    val paramKind = ParamKind.fromValue(kind)
    return ParamSpec(
        key = key.orEmpty(),
        kind = paramKind,
        value = value.toParamValue(paramKind),
        defaultValue = defaultValue.toParamValue(paramKind),
        unit = unit,
        min = min,
        max = max,
    )
}

/** 인증 스냅샷 (생성/상세 응답 verification). */
@Serializable
data class VerificationConfigDto(
    @SerialName("selectedMethod")
    val selectedMethod: String? = null,
    @SerialName("verificationType")
    val verificationType: String? = null,
    @SerialName("signalSource")
    val signalSource: String? = null,
    @SerialName("wearableReq")
    val wearableReq: String? = null,
    @SerialName("requiredPermissions")
    val requiredPermissions: List<String>? = null,
    @SerialName("externalService")
    val externalService: String? = null,
)

internal fun VerificationConfigDto.toDomain(): VerificationConfig =
    VerificationConfig(
        selectedMethod = SelectedMethod.fromValue(selectedMethod) ?: SelectedMethod.MANUAL,
        verificationType = VerificationType.fromValue(verificationType) ?: VerificationType.MANUAL,
        signalSource = SignalSource.fromValue(signalSource),
        wearableRequirement = WearableRequirement.fromValue(wearableReq),
        requiredPermissions = requiredPermissions.orEmpty(),
        externalService = externalService,
    )

/** [kind] 에 따라 JsonElement → ParamValue. */
internal fun JsonElement?.toParamValue(kind: ParamKind): ParamValue =
    when (kind) {
        ParamKind.NUMBER -> ParamValue.Num(this?.jsonPrimitive?.doubleOrNull ?: 0.0)
        ParamKind.TIME -> ParamValue.Text(this?.jsonPrimitive?.content.orEmpty())
    }

/** 생성/상세 응답의 params 는 평탄한 {key: value} (kind 없음) → JSON primitive 타입으로 추론. */
internal fun JsonObject?.toParamValueMap(): Map<String, ParamValue> =
    this.orEmpty().mapValues { (_, element) ->
        val primitive = (element as? JsonPrimitive)
        when {
            primitive == null -> ParamValue.Text(element.toString())
            primitive.isString -> ParamValue.Text(primitive.content)
            primitive.doubleOrNull != null -> ParamValue.Num(primitive.double)
            else -> ParamValue.Text(primitive.content)
        }
    }

/** ParamValue → 요청 직렬화용 JsonPrimitive. */
internal fun ParamValue.toJson(): JsonPrimitive =
    when (this) {
        is ParamValue.Num -> JsonPrimitive(value)
        is ParamValue.Text -> JsonPrimitive(value)
    }
