package com.ruleup.domain.entity.user

/**
 * 계정 티어. 가입 시 [BRONZE] 10점으로 시작한다.
 *
 * 구간은 **회원당 누적 점수 0~2,000 단일 축**을 자른 것이다(점수·티어 정책 §1.1, 2026-08-26 개정).
 * 티어마다 0~99 로 다시 매기던 구모델이 아니다 — 승급해도 초과 점수를 버리지 않는다.
 *
 * 승·강등 판정은 서버가 한다. 여기 구간은 **화면이 띠를 그리기 위한 표**이고, 유예 밴드(표시 티어가
 * 실제 티어보다 높은 상태)는 이 표로 계산되지 않는다 — `MyTier.displayTier` 를 그대로 쓴다.
 */
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
        /** 미지 값은 최하위로 떨어뜨린다 — 서버 enum 확장이 방 입장 판정을 부풀리면 안 된다. */
        fun fromValue(value: String?): Tier = entries.find { it.value == value } ?: BRONZE

        /**
         * 점수가 속한 구간. 상한을 넘는 점수는 [RUBY] 로 본다 — 서버가 상한을 올려도 화면이 빈칸이
         * 되지 않게 하기 위해서다.
         */
        fun ofScore(score: Int): Tier = entries.lastOrNull { score >= it.minScore } ?: BRONZE
    }
}
