package com.ruleup.observability.domain.model

/** 화면 진입 직전의 출처. 유입 경로 분석용이다. */
data class Referrer(
    val fromScreen: ScreenKey,
    val element: ElementKey?,
    val elementState: Attributes,
)
