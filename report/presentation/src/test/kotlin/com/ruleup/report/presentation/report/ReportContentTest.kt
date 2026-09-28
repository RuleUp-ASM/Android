package com.ruleup.report.presentation.report

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.report.presentation.clickPastGuard
import com.ruleup.report.presentation.report.viewmodel.ReportIntent
import com.ruleup.report.presentation.report.viewmodel.ReportState
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 신고 화면. */
@RunWith(RobolectricTestRunner::class)
class ReportContentTest {
    @get:Rule val compose = createComposeRule()

    private val intents = mutableListOf<ReportIntent>()

    @Test
    fun `사유를 고르기 전에는 신고를 접수하지 않는다`() {
        show(state(selected = null))

        compose.onNode(hasText("신고하기") and hasClickAction()).clickPastGuard()

        assertTrue(intents.none { it is ReportIntent.Submit })
    }

    @Test
    fun `사유를 고르면 접수할 수 있다`() {
        show(state(selected = ReportReason.forUser.first()))

        compose.onNode(hasText("신고하기") and hasClickAction()).clickPastGuard()

        assertEquals(listOf<ReportIntent>(ReportIntent.Submit), intents)
    }

    @Test
    fun `접수 중에는 같은 신고를 다시 보내지 않는다`() {
        show(state(selected = ReportReason.forUser.first(), isSubmitting = true))

        compose.onNode(hasText("접수 중") and hasClickAction()).clickPastGuard()

        assertTrue(intents.isEmpty())
    }

    @Test
    fun `누구를 신고하는지 화면에 남는다`() {
        show(state(selected = null))

        compose.onNodeWithText("지현").assertIsDisplayed()
    }

    private fun state(
        selected: ReportReason?,
        isSubmitting: Boolean = false,
    ) = ReportState(
        targetName = "지현",
        reasons = ReportReason.forUser,
        selected = selected,
        isSubmitting = isSubmitting,
        done = null,
    )

    private fun show(state: ReportState) {
        compose.setContent {
            RuleUpTheme {
                ReportContent(state = state, onIntent = { intents += it })
            }
        }
    }
}
