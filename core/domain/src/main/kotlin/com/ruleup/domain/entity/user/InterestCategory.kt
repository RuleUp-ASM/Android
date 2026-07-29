package com.ruleup.domain.entity.user

enum class InterestCategory(
    val value: String,
    val label: String,
    val emoji: String,
) {
    EXERCISE("EXERCISE", "운동", "🏃"),
    READING("READING", "독서", "📚"),
    MEDITATION("MEDITATION", "명상", "🧘"),
    HEALTH("HEALTH", "건강", "💧"),
    WAKE_UP("WAKE_UP", "기상", "🌅"),
    WORK("WORK", "업무", "💼"),
    STUDY("STUDY", "학습", "📖"),
    HOBBY("HOBBY", "취미", "🎨"),
    COOKING("COOKING", "요리", "🍳"),
    FINANCE("FINANCE", "재테크", "💰"),
    ENVIRONMENT("ENVIRONMENT", "환경", "🌱"),
    RELATIONSHIP("RELATIONSHIP", "관계", "🤝"),
    MUSIC("MUSIC", "음악", "🎵"),
    WRITING("WRITING", "글쓰기", "✍️"),
    CODING("CODING", "코딩", "💻"),
    ;

    companion object {
        fun fromValue(value: String): InterestCategory? = entries.find { it.value == value }
    }
}

fun List<String>?.toInterestCategories(): List<InterestCategory> = this.orEmpty().mapNotNull(InterestCategory::fromValue)
