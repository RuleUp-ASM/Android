package com.ruleup.challenge.domain.entity

/** 인증 방식. */
enum class VerificationType(
    val value: String,
) {
    AUTO("AUTO"),
    MANUAL("MANUAL"),
    ;

    /** 신호를 자동 수집해 판정하는가. */
    val isAuto: Boolean
        get() = this == AUTO

    companion object {
        fun fromValue(value: String?): VerificationType? = entries.find { it.value == value }
    }
}

/** 자동 인증 신호원. */
enum class VerificationMethod(
    val value: String,
) {
    // 지정 장소 체류
    GPS_PRESENCE("GPS_PRESENCE"),

    // 지정 장소 회피
    GPS_AVOID("GPS_AVOID"),

    // 대상 앱 사용 시간 상한
    SCREEN_TIME_MAX("SCREEN_TIME_MAX"),

    // 대상 앱 사용 시간 하한
    SCREEN_TIME_MIN("SCREEN_TIME_MIN"),

    // 걸음 수 등 건강 데이터
    HEALTH("HEALTH"),

    // 기상
    WAKE("WAKE"),

    // 취침
    SLEEP("SLEEP"),

    // 수동
    SELF_CHECK("SELF_CHECK"),
    ;

    companion object {
        fun fromValue(value: String?): VerificationMethod? = entries.find { it.value == value }
    }
}

/** 챌린지에 박히는 인증 스냅샷. */
data class VerificationConfig(
    val type: VerificationType,
    val method: VerificationMethod,
    // 표시 문구(예: "기상 06:00 ±10분 내 10걸음").
    val detail: String? = null,
    val requiredPermissions: List<String> = emptyList(),
)

/** 목표값 입력 종류 */
enum class ParamKind(
    val value: String,
) {
    NUMBER("NUMBER"),
    TIME("TIME"),
    ;

    companion object {
        fun fromValue(value: String?): ParamKind = entries.find { it.value == value } ?: NUMBER
    }
}

/** 수정 가능한 목표값 스펙. */
data class ParamSpec(
    val key: String,
    val value: String,
    // 되돌리기용 템플릿 기본값
    val defaultValue: String,
    val kind: ParamKind,
    val unit: String?,
    val min: Double?,
    val max: Double?,
)

/** 이 스펙이 허용하는 값으로 접는다. */
fun ParamSpec.clamp(value: Double): Double =
    value
        .coerceAtLeast(min ?: Double.NEGATIVE_INFINITY)
        .coerceAtMost(max ?: Double.POSITIVE_INFINITY)

/** 지금 값이 허용 범위 안인가. */
val ParamSpec.isInRange: Boolean
    get() {
        if (kind == ParamKind.TIME) return runCatching { java.time.LocalTime.parse(value) }.isSuccess
        val number = value.toDoubleOrNull() ?: return false
        return number >= (min ?: Double.NEGATIVE_INFINITY) && number <= (max ?: Double.POSITIVE_INFINITY)
    }

/** 허용 범위 안내 문구. */
fun ParamSpec.rangeLabel(): String? =
    when {
        min != null && max != null -> "${min.trimZero()} ~ ${max.trimZero()}"
        min != null -> "${min.trimZero()} 이상"
        max != null -> "${max.trimZero()} 이하"
        else -> null
    }

private fun Double.trimZero(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

/** 생성·수정 요청에 실리는 목표값. */
data class ParamEntry(
    val key: String,
    val value: String,
)

/** 장소 체류·앱 사용 목표 시간(분)의 키. */
const val PARAM_DURATION_MIN = "duration_min"

/** 목표 체류 시간(분). */
fun List<ParamSpec>.durationMinutes(): Int? =
    find { it.key == PARAM_DURATION_MIN }
        ?.value
        ?.toDoubleOrNull()
        ?.toInt()
        ?.takeIf { it > 0 }

/** [ParamSpec] 의 현재값만 뽑아 요청 형태로 접는다. */
fun List<ParamSpec>.toEntries(): List<ParamEntry> = map { ParamEntry(key = it.key, value = it.value) }

/** 초안 생성 rate limit 초과. */
class RecommendationRateLimitedException(
    val retryAfterSeconds: Int? = null,
) : Exception("초안 생성 요청이 너무 잦습니다.")

/** 초안이 만료·소실됐다. */
class DraftExpiredException : Exception("초안이 만료되었습니다.")
