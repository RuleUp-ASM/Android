package com.ruleup.ui.helper

import androidx.compose.runtime.staticCompositionLocalOf
import com.ruleup.observability.domain.api.Observability

/** 생성자 주입을 못 받는 Composable 용 [Observability] 경로. */
val LocalObservability =
    staticCompositionLocalOf<Observability> { error("LocalObservability 가 제공되지 않았습니다.") }
