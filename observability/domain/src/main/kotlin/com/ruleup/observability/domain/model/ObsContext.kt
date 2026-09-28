package com.ruleup.observability.domain.model

/** 이벤트 발생 시점의 동적 컨텍스트. */
data class ObsContext(
    val currentScreen: ScreenKey?,
)
