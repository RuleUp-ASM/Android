package com.ruleup.domain.entity.category

/**
 * 루틴 분류. **관심사(내가 고른 분야)와 챌린지 분류에 같은 값 집합을 쓴다** — 서버가 둘을
 * 구분하지 않으므로 타입도 하나로 둔다.
 *
 * [label] 은 표시 문자열이면서 동시에 **서버 응답 매칭 키**다. 탐색 카테고리 API 가 표시명만
 * 내려주기 때문에 `challenge:data` 가 이 값으로 매칭한다(`ExploreResponse`). 그래서 UI 관심사처럼
 * 보여도 designsystem 으로 옮기면 안 된다 — data 레이어가 designsystem 을 의존하게 되고,
 * 옮기는 순간 탐색 화면 아이콘이 조용히 기본값으로 떨어진다.
 *
 * 표시용 이모지·색·아이콘은 `core:designsystem` 의 CategoryVisuals 에 있다.
 */
enum class Category(
    val value: String,
    val label: String,
) {
    EXERCISE("EXERCISE", "운동"),
    READING("READING", "독서"),
    MEDITATION("MEDITATION", "명상"),
    HEALTH("HEALTH", "건강"),
    WAKE_UP("WAKE_UP", "기상"),
    WORK("WORK", "업무"),
    STUDY("STUDY", "학습"),
    HOBBY("HOBBY", "취미"),
    COOKING("COOKING", "요리"),
    FINANCE("FINANCE", "재테크"),
    ENVIRONMENT("ENVIRONMENT", "환경"),
    RELATIONSHIP("RELATIONSHIP", "관계"),
    MUSIC("MUSIC", "음악"),
    WRITING("WRITING", "글쓰기"),
    CODING("CODING", "코딩"),
    ;

    companion object {
        fun fromValue(value: String): Category? = entries.find { it.value == value }
    }
}

fun List<String>?.toCategories(): List<Category> = this.orEmpty().mapNotNull(Category::fromValue)
