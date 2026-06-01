package com.ruleup.core.model

/**
 * 관심 카테고리. [value] 는 서버와 주고받는 코드, [label] 은 화면 표시용 한글.
 */
enum class InterestCategory(
    val value: String,
    val label: String,
    val emogi: String,
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
