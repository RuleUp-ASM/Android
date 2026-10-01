package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.detail.component.ReportDoneSheet
import com.ruleup.challenge.presentation.detail.component.ReportReasonSheet
import com.ruleup.challenge.presentation.detail.component.label
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.ReportReason
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ReportReasonSheetTest {
    @get:Rule val compose = createComposeRule()

    private val selected = mutableListOf<ReportReason>()
    private var submits = 0

    private fun show(
        reasons: List<ReportReason> = ReportReason.forChallenge,
        chosen: ReportReason? = null,
        submitting: Boolean = false,
    ) {
        compose.setContent {
            RuleUpTheme {
                ReportReasonSheet(
                    title = "이 챌린지를 신고할까요?",
                    reasons = reasons,
                    selected = chosen,
                    submitting = submitting,
                    onSelect = { selected += it },
                    onSubmit = { submits++ },
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun `챌린지 신고에는 부정 인증 의심 사유가 보이지 않는다`() {
        show()

        compose.onNodeWithText(ReportReason.CHEATING_SUSPECT.label()).assertDoesNotExist()
    }

    @Test
    fun `사용자 신고에는 부정 인증 의심 사유가 보인다`() {
        show(reasons = ReportReason.forUser)

        compose.onNodeWithText(ReportReason.CHEATING_SUSPECT.label()).assertIsDisplayed()
    }

    @Test
    fun `사유를 고르기 전에는 신고 버튼을 누를 수 없다`() {
        show(chosen = null)

        compose.onNodeWithText("신고하기").assertIsNotEnabled()
    }

    @Test
    fun `사유를 고르면 신고 버튼이 열린다`() {
        show(chosen = ReportReason.SPAM_AD)

        compose.onNodeWithText("신고하기").assertIsEnabled()
    }

    @Test
    fun `사유를 누르면 그 사유가 선택으로 올라간다`() {
        show()

        compose.onNodeWithText(ReportReason.SPAM_AD.label()).clickPastGuard()

        assertEquals(listOf(ReportReason.SPAM_AD), selected)
    }

    @Test
    fun `접수 중에는 다시 누를 수 없고 진행 중임을 알린다`() {
        show(chosen = ReportReason.SPAM_AD, submitting = true)

        compose.onNodeWithText("접수 중").assertIsNotEnabled()
    }

    @Test
    fun `자유 입력칸을 두지 않는다`() {
        show()

        compose.onNodeWithText("신고 사유를 입력해 주세요").assertDoesNotExist()
    }
}

@RunWith(RobolectricTestRunner::class)
class ReportDoneSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `무엇이 가려졌는지 알려준다`() {
        compose.setContent {
            RuleUpTheme {
                ReportDoneSheet(effectMessage = "이 챌린지가 탐색 목록에서 빠졌어요.", onDismiss = {})
            }
        }

        compose.onNodeWithText("신고를 접수했어요").assertIsDisplayed()
        compose.onNodeWithText("이 챌린지가 탐색 목록에서 빠졌어요.").assertIsDisplayed()
    }

    @Test
    fun `차단을 어디서 푸는지 알려준다`() {
        // 접수 화면이 알려주지 않으면 사용자가 목록을 찾아갈 방법이 없다.
        compose.setContent {
            RuleUpTheme { ReportDoneSheet(effectMessage = "가렸어요.", onDismiss = {}) }
        }

        compose.onNodeWithText("차단은 내 화면에만 적용돼요 · 마이에서 풀 수 있어요").assertIsDisplayed()
    }
}
