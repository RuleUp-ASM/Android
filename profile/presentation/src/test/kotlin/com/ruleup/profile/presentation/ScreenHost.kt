package com.ruleup.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performClick
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.test.ClickClock
import com.ruleup.domain.test.RecordingMessageHelper
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.LocalObservability
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** 마이 탭 화면 렌더 준비. 화면이 소비하는 CompositionLocal 을 테스트가 직접 채운다. */
fun ComposeContentTestRule.renderScreen(
    nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    messages: RecordingMessageHelper = RecordingMessageHelper(),
    content: @Composable () -> Unit,
): RecordingNavigationHelper {
    setContent {
        RuleUpTheme {
            CompositionLocalProvider(
                LocalNavigationHelper provides nav,
                LocalMessageHelper provides messages,
                LocalObservability provides testObservability(),
            ) {
                content()
            }
        }
    }
    return nav
}

/** 전역 클릭 가드를 넘겨 누른다 — 자세한 이유는 [ClickClock]. */
fun SemanticsNodeInteraction.clickPastGuard() {
    ShadowSystemClock.advanceBy(Duration.ofMillis(ClickClock.nextOffsetMillis()))
    performClick()
}
