package com.ruleup.challenge.domain.entity

/**
 * 루틴 인증 방식 (명세: verificationMethods 다중선택 → 단일 selectedMethod 로 대체).
 * AUTO = 자동 인증(권한·신호원 필요), MANUAL = 수동(직접 체크).
 */
enum class SelectedMethod(
    val value: String,
) {
    AUTO("AUTO"),
    MANUAL("MANUAL"),
    ;

    companion object {
        fun fromValue(value: String?): SelectedMethod? = entries.find { it.value == value }
    }
}

/** 인증 수단 종류. */
enum class VerificationType(
    val value: String,
) {
    PHONE("PHONE"),
    HEALTH_CONNECT("HEALTH_CONNECT"),
    EXTERNAL("EXTERNAL"),
    MANUAL("MANUAL"),
    ;

    companion object {
        fun fromValue(value: String?): VerificationType? = entries.find { it.value == value }
    }
}

/** 자동 인증 신호원. 서버가 새 값을 추가해도 깨지지 않도록 미지정/미인식은 UNKNOWN 으로 떨어진다. */
enum class SignalSource {
    GPS,
    GEOFENCE,
    PHOTO,
    GROUP_CHECK,
    UNKNOWN,
    ;

    companion object {
        fun fromValue(value: String?): SignalSource? =
            when (value) {
                null -> null
                else -> entries.find { it.name == value } ?: UNKNOWN
            }
    }
}

/** 웨어러블 요구 수준 (안내용, 후순위). */
enum class WearableRequirement(
    val value: String,
) {
    NONE("NONE"),
    OPTIONAL("OPTIONAL"),
    REQUIRED("REQUIRED"),
    ;

    companion object {
        fun fromValue(value: String?): WearableRequirement = entries.find { it.value == value } ?: NONE
    }
}

/** 목표값 입력 종류 (UI 입력기 분기용). */
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

/**
 * 목표값 (서버 params 의 Number|String). [Any] 없이 [ParamKind] 로 구분해 표현한다.
 */
sealed interface ParamValue {
    data class Num(
        val value: Double,
    ) : ParamValue

    data class Text(
        val value: String,
    ) : ParamValue
}

/**
 * 추천이 내려주는 선택 가능한 인증 옵션 (명세 recommendation.options[]).
 */
data class VerificationOption(
    val method: SelectedMethod,
    // 기본 선택 여부
    val recommended: Boolean,
    val verificationType: VerificationType,
    val signalSource: SignalSource?,
    val wearableRequirement: WearableRequirement,
    val externalService: String?,
    // AUTO 에 필요한 권한 토큰 목록 (서버 정의)
    val requiredPermissions: List<String>,
)

/**
 * 수정 가능한 목표값 스펙 (명세 recommendation.params[]).
 * [value] 는 사용자가 편집하는 현재값, [defaultValue] 는 되돌리기용 템플릿 기본값.
 */
data class ParamSpec(
    val key: String,
    val kind: ParamKind,
    val value: ParamValue,
    val defaultValue: ParamValue,
    val unit: String?,
    val min: Double?,
    val max: Double?,
)

/**
 * 생성/상세 챌린지에 박히는 인증 스냅샷 (명세 verification). 템플릿이 바뀌어도 보존된다.
 */
data class VerificationConfig(
    val selectedMethod: SelectedMethod,
    val verificationType: VerificationType,
    val signalSource: SignalSource?,
    val wearableRequirement: WearableRequirement,
    val requiredPermissions: List<String>,
    val externalService: String?,
)

/**
 * AUTO 생성 시 서버가 권한 부족(ROUTINE_PERMISSION_REQUIRED)으로 바운스했음을 알리는 도메인 예외.
 * 데이터 계층이 ApiException 을 이 타입으로 변환해, 프레젠테이션이 네트워크 계층을 모르고도 분기할 수 있게 한다.
 */
class ChallengePermissionRequiredException(
    // 서버가 부족 권한 목록을 주면 담고, 없으면 null (호출자가 옵션 전체를 재요청).
    val requiredPermissions: List<String>? = null,
) : Exception("자동 인증에 필요한 권한이 부족합니다.")

/**
 * 추천 rate limit 초과 (명세 recommendation, HTTP 429 RECOMMENDATION_RATE_LIMITED).
 * 화면은 최초 생성 화면으로 복귀하고 [retryAfterSeconds] 후 재시도 안내한다.
 */
class RecommendationRateLimitedException(
    val retryAfterSeconds: Int? = null,
) : Exception("추천 요청이 너무 잦습니다.")
