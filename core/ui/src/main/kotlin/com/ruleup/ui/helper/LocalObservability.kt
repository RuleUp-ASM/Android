package com.ruleup.ui.helper

import androidx.compose.runtime.staticCompositionLocalOf
import com.ruleup.observability.domain.api.Observability

/**
 * 생성자 주입을 못 받는 Composable 용 [Observability] 경로. 값은 `:app` 의 AppRoot 가 넣는다.
 * ViewModel 은 이걸 쓰지 않는다 — 생성자로 받아 의존을 시그니처에 드러낸다.
 */
val LocalObservability =
    staticCompositionLocalOf<Observability> { error("LocalObservability 가 제공되지 않았습니다.") }
