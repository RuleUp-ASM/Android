package com.ruleup.onboarding.domain.entity

/**
 * 성별. **API 필수 필드**다.
 *
 * UI 는 건너뛰기를 허용하되, 건너뛴 경우 클라가 [NON_BINARY] 를 보낸다(2026-08-03 확정). 필드를
 * 빼면 서버가 `GENDER_REQUIRED` 로 400 을 준다.
 */
enum class Gender(
    val value: String,
) {
    MALE("MALE"),
    FEMALE("FEMALE"),
    NON_BINARY("NON_BINARY"),
    ;

    companion object {
        fun fromValue(value: String?): Gender? = entries.find { it.value == value }
    }
}
