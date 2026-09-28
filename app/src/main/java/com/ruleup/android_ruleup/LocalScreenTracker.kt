package com.ruleup.android_ruleup

import androidx.compose.runtime.staticCompositionLocalOf
import com.ruleup.android_ruleup.observability.ScreenTracker

/** 컴포지션 전역 [ScreenTracker]. */
val LocalScreenTracker =
    staticCompositionLocalOf<ScreenTracker> { error("LocalScreenTracker 가 제공되지 않았습니다.") }
