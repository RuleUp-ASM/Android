package com.ruleup.onboarding.domain.entity

/**
 * 계정 티어. 가입 시 [BRONZE] 10점으로 시작한다.
 *
 * 점수 계산과 승·강등 규칙은 티어 스펙 소관이라 여기서는 응답 값만 다룬다.
 */
enum class Tier(
    val value: String,
) {
    BRONZE("BRONZE"),
    SILVER("SILVER"),
    GOLD("GOLD"),
    DIAMOND("DIAMOND"),
    RUBY("RUBY"),
    ;

    companion object {
        /** 미지 값은 최하위로 떨어뜨린다 — 서버 enum 확장이 방 입장 판정을 부풀리면 안 된다. */
        fun fromValue(value: String?): Tier = entries.find { it.value == value } ?: BRONZE
    }
}
