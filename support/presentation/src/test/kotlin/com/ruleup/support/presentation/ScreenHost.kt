package com.ruleup.support.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performClick
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.test.ClickClock
import com.ruleup.domain.test.RecordingMessageHelper
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** 문의 화면 렌더 준비. */
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
            ) {
                content()
            }
        }
    }
    return nav
}

/** 테스트 클릭 간격 확보. */
fun SemanticsNodeInteraction.clickPastGuard() {
    ShadowSystemClock.advanceBy(Duration.ofMillis(ClickClock.nextOffsetMillis()))
    performClick()
}
