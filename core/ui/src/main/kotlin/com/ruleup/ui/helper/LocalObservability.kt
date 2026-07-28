package com.ruleup.ui.helper

import androidx.compose.runtime.staticCompositionLocalOf
import com.ruleup.observability.domain.api.Observability

/**
 * 그래프의 [Observability] 를 컴포지션 전역으로 제공한다(`:app` 의 AppRoot 에서 주입).
 *
 * Composable 은 생성자 주입을 받을 수 없어 이 경로가 필요하다. `core:ui` 에 두는 이유는
 * `core:map`·각 feature 의 presentation 이 모두 이 모듈을 보기 때문이다 — `:app` 에 두면
 * 하위 모듈이 접근할 수 없다.
 *
 * ViewModel 은 이걸 쓰지 않고 생성자로 직접 주입받는다. 의존이 시그니처에 드러나는 편이 낫다.
 */
val LocalObservability =
    staticCompositionLocalOf<Observability> { error("LocalObservability 가 제공되지 않았습니다.") }
