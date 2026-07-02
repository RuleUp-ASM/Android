package com.ruleup.android_ruleup.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState

/**
 * 현재 화면(네비 경로)을 JankStats 의 [PerformanceMetricsState] 에 "screen" 상태로 주입한다.
 * 그러면 janky 프레임 로그([com.ruleup.android_ruleup.MainActivity])의 states 에 화면 이름이 함께 찍혀
 * "어느 화면에서 jank 가 났는지" 알 수 있다. 디버그 빌드에서만 호출한다.
 *
 * [screen] 이 바뀌면 상태를 갱신하고, 컴포지션을 떠날 때 정리한다.
 */
@Composable
fun TrackJankScreen(screen: String) {
    val view = LocalView.current
    val holder = remember(view) { PerformanceMetricsState.getHolderForHierarchy(view) }
    DisposableEffect(holder, screen) {
        holder.state?.putState(KEY_SCREEN, screen)
        onDispose { holder.state?.removeState(KEY_SCREEN) }
    }
}

private const val KEY_SCREEN = "screen"
