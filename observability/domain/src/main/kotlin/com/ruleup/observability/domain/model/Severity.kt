package com.ruleup.observability.domain.model

/**
 * 이벤트 심각도. **비교에는 반드시 [atLeast] 를 쓴다** — enum 의 기본 비교는 [level] 이 아니라
 * 선언 순서라, 항목을 재정렬하거나 중간에 끼우는 순간 두 순서가 조용히 갈라진다.
 *
 * [level] 에 10 단위 간격을 둔 것도 중간 삽입을 순서 변경 없이 흡수하기 위해서다.
 */
enum class Severity(
    val level: Int,
) {
    VERBOSE(10),
    DEBUG(20),
    INFO(30),
    WARN(40),
    ERROR(50),
}

/** [this] 가 [other] 이상인지. [Severity] 비교의 유일한 정식 수단이다. */
infix fun Severity.atLeast(other: Severity) = level >= other.level
