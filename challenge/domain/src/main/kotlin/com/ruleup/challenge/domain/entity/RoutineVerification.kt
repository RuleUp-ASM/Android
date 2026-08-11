package com.ruleup.challenge.domain.entity

/**
 * 인증 방식 (명세 `verification.type`). 방 단위로 고정되며 **AUTO → MANUAL 단방향 전환만** 허용한다.
 * 역방향(MANUAL → AUTO)은 서버가 `ROUTINE_AUTO_NOT_SUPPORTED` 로 막는다.
 */
enum class VerificationType(
    val value: String,
) {
    AUTO("AUTO"),
    MANUAL("MANUAL"),
    ;

    companion object {
        fun fromValue(value: String?): VerificationType? = entries.find { it.value == value }
    }
}

/**
 * 자동 인증 신호원 (명세 `verification.method`).
 *
 * 구 `SCREEN_TIME` 은 상한/하한이 갈려 `SCREEN_TIME_MAX`·`SCREEN_TIME_MIN` 이 됐고, `GPS_AVOID`·`SLEEP`
 * 이 새로 생겼다(2026-08-11 명세). 구 `PHOTO` 는 폐기됐다.
 *
 * 서버가 값을 추가해도 구버전 앱이 통째로 막히지 않도록 미인식 값은 [SELF_CHECK] 로 떨어뜨린다 —
 * 모르는 자동 인증을 자동으로 처리하는 척하는 것보다 수동으로 보이는 편이 안전하다.
 */
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

    // 수동 — 직접 체크
    SELF_CHECK("SELF_CHECK"),
    ;

    companion object {
        fun fromValue(value: String?): VerificationMethod? = entries.find { it.value == value }
    }
}

/**
 * 챌린지에 박히는 인증 스냅샷 (명세 `verification`).
 *
 * [requiredPermissions] 는 **가입·생성 전에 클라이언트가 확보해야 하는** OS 권한 목록이다(수동 방이면 빈 배열).
 * 서버는 권한 보유 여부를 게이트로 검사하지 않는다 — Android 권한 상태를 서버가 신뢰성 있게 알 수 없고
 * 가입 후 언제든 철회될 수 있기 때문. 가입 후 권한 거부를 탈퇴로 롤백하는 경로는 폐기됐다.
 */
data class VerificationConfig(
    val type: VerificationType,
    val method: VerificationMethod,
    // 표시 문구(예: "기상 06:00 ±10분 내 10걸음"). 공개 상세에서만 내려온다.
    val detail: String? = null,
    val requiredPermissions: List<String> = emptyList(),
)

/** 목표값 입력 종류 — 입력 위젯 분기용. 미인식 값은 [NUMBER] 로 떨어진다. */
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
 * 수정 가능한 목표값 스펙 (명세 `draft.params[]` · `settings.config.params[]`).
 *
 * **루틴별 분기를 클라이언트에 하드코딩하지 않는다** — 입력 위젯과 검증 범위를 [kind]·[unit]·[min]·[max]
 * 로 결정한다. 루틴 테이블이 서버에서 계속 늘어나기 때문.
 *
 * 값은 전선(wire)과 같이 문자열이다 — `"06:00"` 처럼 숫자가 아닌 값이 섞여 있어 숫자 타입으로 좁히면
 * 표현이 깨진다. 숫자 해석이 필요한 곳은 [kind] 를 보고 파싱한다.
 */
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

/** 생성·수정 요청에 실리는 목표값 (명세 `params[]` — `{key, value}` 만). */
data class ParamEntry(
    val key: String,
    val value: String,
)

/** [ParamSpec] 의 현재값만 뽑아 요청 형태로 접는다. */
fun List<ParamSpec>.toEntries(): List<ParamEntry> = map { ParamEntry(key = it.key, value = it.value) }

/**
 * 초안 생성 rate limit 초과 (명세 429 `RECOMMENDATION_RATE_LIMITED` — 사용자당 1분 10회).
 *
 * 화면은 [retryAfterSeconds] 카운트다운을 버튼에 표시하고 비활성한다. **자동 재시도는 금지** —
 * 남은 rate limit 을 소진시킨다. 추천 칩 경로는 제한이 없으므로 대안으로 안내한다.
 */
class RecommendationRateLimitedException(
    val retryAfterSeconds: Int? = null,
) : Exception("초안 생성 요청이 너무 잦습니다.")

/**
 * 초안이 만료·소실됐다 (명세 400 `DRAFT_NOT_FOUND` / `DRAFT_EXPIRED`).
 * 서버는 초안을 24시간만 보관한다 — 화면은 초안 재생성을 안내한다.
 */
class DraftExpiredException : Exception("초안이 만료되었습니다.")
