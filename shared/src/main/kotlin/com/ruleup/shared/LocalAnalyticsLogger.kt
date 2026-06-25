package com.ruleup.shared

import androidx.compose.runtime.staticCompositionLocalOf
import com.ruleup.analytics.AnalyticsLogger

/**
 * 그래프의 [AnalyticsLogger] 를 컴포지션 전역으로 제공한다([AppRoot] 에서 주입).
 * 화면 진입 추적(ScreenView) 등 UI 계층에서 직접 이벤트를 기록할 때 사용한다.
 */
val LocalAnalyticsLogger =
    staticCompositionLocalOf<AnalyticsLogger> { error("LocalAnalyticsLogger 가 제공되지 않았습니다.") }
