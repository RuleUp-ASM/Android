package com.ruleup.domain.entity.category

/**
 * 관심 분야 선택 상한 (명세). **가입과 프로필 수정이 같은 값을 본다** — 프로필은 서버가
 * `maxSelectable` 을 함께 주므로 그 값을 쓰고, 이 상수는 응답이 비었을 때의 기본값이다.
 *
 * 하한은 없다. 가입에서는 아무것도 안 고르고 넘어갈 수 있다.
 */
object InterestLimits {
    const val MAX = 6
}

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
         * `TIDYING`·`CAREER` 별칭은 제거했다 — 탐색 기능 스펙이 2026-08-11 에 12종을 관심 분야 정책
         * 표기(`HOUSEKEEPING`·`CAREER_PRODUCTIVITY`)로 확정하고 구 표기를 폐기했다. 서버가 아직
         * 구 표기를 내려주면 그 분류만 조용히 사라지므로, 이 제거는 서버 정렬을 전제로 한다.
         */
        fun fromValue(value: String): Category? = entries.find { it.value == value } ?: LEGACY_ALIASES[value]

        /**
         * 서버가 아직 내려주는 15종 시절 code.
         *
         * 12종 확정 전 표기(`TIDYING`·`CAREER`)와 달리, 이 값들은 **폐기가 확인되지 않았다**.
         * 별칭을 빼면 해당 분류가 조용히 null 로 떨어져 탐색·홈 카드의 아이콘과 필터가 사라지므로,
         * 서버 응답에서 사라진 것을 확인하기 전까지 남긴다.
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
