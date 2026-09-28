package com.ruleup.domain.entity.category

/** 관심 분야 선택 상한. */
object InterestLimits {
    const val MAX = 6
}

/** 루틴 분류. */
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
        /** 서버 code 를 카테고리로 옮긴다. */
        fun fromValue(value: String): Category? = entries.find { it.value == value } ?: LEGACY_ALIASES[value]

        /** 서버가 아직 내려주는 15종 시절 code. */
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
