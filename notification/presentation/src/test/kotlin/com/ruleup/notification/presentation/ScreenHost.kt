package com.ruleup.notification.presentation

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import com.ruleup.domain.test.ClickClock
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** 테스트 클릭 간격 확보. */
fun SemanticsNodeInteraction.clickPastGuard() {
    ShadowSystemClock.advanceBy(Duration.ofMillis(ClickClock.nextOffsetMillis()))
    performClick()
}
