package com.ruleup.observability.domain.model

/** 이벤트 심각도. */
enum class Severity(
    val level: Int,
) {
    VERBOSE(10),
    DEBUG(20),
    INFO(30),
    WARN(40),
    ERROR(50),
}

/** [this] 가 [other] 이상인지. */
infix fun Severity.atLeast(other: Severity) = level >= other.level
