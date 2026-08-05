package com.ruleup.domain.entity.category

/**
 * 루틴 분류. **관심사(내가 고른 분야)와 챌린지 분류에 같은 값 집합을 쓴다** — 서버가 둘을
 * 구분하지 않으므로 타입도 하나로 둔다.
 *
 * 값과 순서는 관심 분야 정책의 12종·노출 순서를 그대로 따른다. `GET /challenge-categories` 가
 * "12개 고정, 정책 순서 그대로"를 계약으로 갖고 온보딩 관심사 화면도 같은 순서라, [entries] 를
 * 그대로 그리면 두 화면의 순서가 저절로 맞는다.
 *
 * [label] 은 **표시 전용**이다. 예전엔 탐색 카테고리 API 가 표시명만 내려줘서 매칭 키를 겸했는데,
 * 지금은 code 를 함께 내려주므로 매칭은 [fromValue] 하나로 일원화됐다.
 *
 * 표시용 이모지·색·아이콘은 `core:designsystem` 의 CategoryVisuals 에 있다.
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
         *
         * [LEGACY_ALIASES] 는 **한시적이다** — 회원가입 API 는 `HOUSEKEEPING`·`CAREER_PRODUCTIVITY`
         * 로 확정됐는데 챌린지 카테고리·탐색 API 는 아직 `TIDYING`·`CAREER` 를 내려준다. 별칭이
         * 없으면 그 둘만 조용히 null 로 떨어져 아이콘·필터가 사라진다. 서버가 정렬되면 지운다.
         */
        fun fromValue(value: String): Category? = entries.find { it.value == value } ?: LEGACY_ALIASES[value]

        // TODO(#185): 서버가 회원가입 API 기준 code 로 정렬하면 제거한다.
        private val LEGACY_ALIASES =
            mapOf(
                "TIDYING" to HOUSEKEEPING,
                "CAREER" to CAREER_PRODUCTIVITY,
            )
    }
}

fun List<String>?.toCategories(): List<Category> = this.orEmpty().mapNotNull(Category::fromValue)
