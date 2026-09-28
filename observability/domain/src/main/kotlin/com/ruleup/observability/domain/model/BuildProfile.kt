package com.ruleup.observability.domain.model

enum class BuildProfile {
    DEV,
    QA,
    PRODUCTION,
    ;

    /** 개발자에게 더 보여줘도 되는 빌드인가 */
    val isDebuggable: Boolean
        get() = this != PRODUCTION
}
