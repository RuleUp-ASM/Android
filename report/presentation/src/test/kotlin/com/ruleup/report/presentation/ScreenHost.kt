package com.ruleup.report.presentation

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import com.ruleup.domain.test.ClickClock
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** 전역 클릭 가드를 넘겨 누른다 — 자세한 이유는 [ClickClock]. */
fun SemanticsNodeInteraction.clickPastGuard() {
    ShadowSystemClock.advanceBy(Duration.ofMillis(ClickClock.nextOffsetMillis()))
    performClick()
}
