package com.ruleup.domain.entity.user

/**
 * 성별. **필수 입력이며 남/여 두 가지다** (회원 정책 §2 · 2026-08-20 확정).
 *
 * 한때 UI 건너뛰기를 허용하고 그 경우 `NON_BINARY` 를 보냈는데, 정책은 성별을 필수로 정의한다.
 * 건너뛰기 경로와 함께 값도 걷어냈다 — 고르지 않으면 다음 단계로 가지 못한다.
 */
enum class Gender(
    val value: String,
) {
    MALE("MALE"),
    FEMALE("FEMALE"),
    ;

    companion object {
        fun fromValue(value: String?): Gender? = entries.find { it.value == value }
    }
}
