package com.ruleup.observability.domain.model

/**
 * 이벤트 심각도. 진단 채널이 주 용도이고, 비즈니스·성능 페이로드는 [INFO] 로 간주된다.
 *
 * **비교에는 반드시 [atLeast] 를 쓴다. `>=`·`compareTo` 를 쓰면 안 된다.**
 * enum 의 기본 비교는 [level] 이 아니라 **선언 순서(ordinal)** 를 따르므로, 누군가 항목을
 * 알파벳순으로 정렬하거나 중간에 새 값을 끼워 넣는 순간 두 순서가 조용히 갈라진다.
 * [level] 값에 간격(10 단위)을 둔 것도 중간 삽입을 순서 변경 없이 흡수하기 위해서다.
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
