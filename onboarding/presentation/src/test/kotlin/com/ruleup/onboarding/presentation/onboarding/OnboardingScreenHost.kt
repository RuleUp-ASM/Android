package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performClick
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.test.ClickClock
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.logging.domain.test.RecordingBizLogger
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.ui.helper.LocalBizLogger
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.LocalObservability
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** 온보딩 화면 렌더 준비. */
fun ComposeContentTestRule.renderOnboarding(
    nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    content: @Composable () -> Unit,
): RecordingNavigationHelper {
    setContent {
        RuleUpTheme {
            CompositionLocalProvider(
                LocalNavigationHelper provides nav,
                LocalObservability provides testObservability(),
                LocalBizLogger provides RecordingBizLogger(),
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
