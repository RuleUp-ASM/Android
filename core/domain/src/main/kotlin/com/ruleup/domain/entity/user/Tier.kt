package com.ruleup.domain.entity.user

/** 계정 티어. */
enum class Tier(
    val value: String,
    val minScore: Int,
    val maxScore: Int,
) {
    BRONZE("BRONZE", minScore = 0, maxScore = 99),
    SILVER("SILVER", minScore = 100, maxScore = 299),
    GOLD("GOLD", minScore = 300, maxScore = 499),
    DIAMOND("DIAMOND", minScore = 500, maxScore = 999),
    RUBY("RUBY", minScore = 1_000, maxScore = 2_000),
    ;

    val scoreRange: IntRange
        get() = minScore..maxScore

    companion object {
        /** 미지 값은 최하위로 떨어뜨린다 */
        fun fromValue(value: String?): Tier = entries.find { it.value == value } ?: BRONZE

        /** 점수가 속한 구간. */
        fun ofScore(score: Int): Tier = entries.lastOrNull { score >= it.minScore } ?: BRONZE
    }
}
