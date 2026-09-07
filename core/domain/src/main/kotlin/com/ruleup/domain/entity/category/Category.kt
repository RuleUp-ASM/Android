package com.ruleup.domain.entity.category

/**
 * 관심 분야 선택 상한(명세). 프로필은 서버가 주는 `maxSelectable` 을 쓰고, 이 값은 응답이 빌 때의 기본값이다.
 * 하한은 없다 — 가입에서 아무것도 안 고르고 넘어갈 수 있다.
 */
object InterestLimits {
    const val MAX = 6
}

/**
 * 루틴 분류. 관심사와 챌린지 분류가 같은 값 집합을 쓴다 — 서버가 둘을 구분하지 않는다.
 *
 * 값과 순서는 관심 분야 정책의 12종·노출 순서 그대로다. `GET /challenge-categories` 도
 * "12개 고정, 정책 순서"가 계약이라 [entries] 를 그리면 온보딩·탐색 순서가 저절로 맞는다.
 *
 * [label] 은 표시 전용이고 매칭은 [fromValue] 뿐이다. 색·아이콘은 `core:designsystem` 의 CategoryVisuals.
 */
enum class Category(
    val value: String,
    val label: String,
) {
    EXERCISE("EXERCISE", "운동"),
    WAKE_SLEEP("WAKE_SLEEP", "기상·수면"),
    DIET_HEALTH("DIET_HEALTH", "식습관·건강"),
    STUDY("STUDY", "학습"),
    READING("READING", "독서"),
    MIND("MIND", "마음"),
    FINANCE("FINANCE", "재테크"),
    HOBBY("HOBBY", "취미"),
    HOUSEKEEPING("HOUSEKEEPING", "정리·살림"),
    CAREER_PRODUCTIVITY("CAREER_PRODUCTIVITY", "커리어·생산성"),
    DETOX("DETOX", "절제·디톡스"),
    ETC("ETC", "기타"),
    ;

    companion object {
        /**
         * 서버 code 를 카테고리로 옮긴다. 아는 값이 아니면 null 이다.
         * 폐기된 구 표기 `TIDYING`·`CAREER` 는 흡수하지 않는다 — 서버가 아직 보내면 그 분류만 조용히 사라진다.
         */
        fun fromValue(value: String): Category? = entries.find { it.value == value } ?: LEGACY_ALIASES[value]

        /**
         * 서버가 아직 내려주는 15종 시절 code. 폐기가 확인되지 않아 남긴다 —
         * 빼면 그 분류가 null 로 떨어져 탐색·홈 카드의 아이콘과 필터가 사라진다.
         */
        private val LEGACY_ALIASES =
            mapOf(
                "WAKE_UP" to WAKE_SLEEP,
                "HEALTH" to DIET_HEALTH,
                "MEDITATION" to MIND,
                "COOKING" to HOUSEKEEPING,
                "WORK" to CAREER_PRODUCTIVITY,
            )
    }
}

fun List<String>?.toCategories(): List<Category> = this.orEmpty().mapNotNull(Category::fromValue)
