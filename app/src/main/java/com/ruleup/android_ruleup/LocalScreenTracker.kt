package com.ruleup.android_ruleup

import androidx.compose.runtime.staticCompositionLocalOf
import com.ruleup.android_ruleup.observability.ScreenTracker

/**
 * 그래프의 [ScreenTracker] 를 컴포지션 전역으로 제공한다([AppRoot] 에서 주입).
 * 화면 진입 추적이 필요한 UI 계층에서 쓴다 — 실사용처는 네비게이션 한 곳이다.
 *
 * 일반 이벤트 기록은 이 로컬을 거치지 않는다. ViewModel 이 `Observability` 를 생성자로 주입받는다.
 */
val LocalScreenTracker =
    staticCompositionLocalOf<ScreenTracker> { error("LocalScreenTracker 가 제공되지 않았습니다.") }
