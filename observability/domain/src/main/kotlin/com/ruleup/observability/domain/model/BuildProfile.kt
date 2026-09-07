package com.ruleup.observability.domain.model

enum class BuildProfile {
    DEV,
    QA,
    PRODUCTION,
    ;

    /** 개발자에게 더 보여줘도 되는 빌드인가 — 상세 로그·실패 즉시 노출의 기준. */
    val isDebuggable: Boolean
        get() = this != PRODUCTION
}
