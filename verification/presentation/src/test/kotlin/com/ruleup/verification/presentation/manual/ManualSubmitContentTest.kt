package com.ruleup.verification.presentation.manual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.presentation.manual.viewmodel.ManualSubmitState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 수동 인증 화면의 렌더 규칙.
 *
 * 이 화면에서 잘못 그려지면 사용자가 **인증하지 않은 하루를 인증한 줄 안다.** 그래서 오늘 상태와
 * CTA 가 서로 어긋나지 않는 것이 곧 기능이다.
 */
@RunWith(RobolectricTestRunner::class)
class ManualSubmitContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `아직 체크 전이면 체크 CTA 를 연다`() {
        render(state(status = TodayResultStatus.IN_PROGRESS))

        compose.onNodeWithText("아직 인증 전이에요").assertExists()
        compose.onNodeWithText("오늘 완료로 체크").assertExists()
    }

    @Test
    fun `체크된 날에는 되돌릴 길만 남긴다`() {
        // 체크된 날에 체크 버튼이 또 있으면 두 번째 제출이 409 로 튕긴다.
        render(state(status = TodayResultStatus.DONE, verificationId = "vf-1"))

        compose.onNodeWithText("오늘 인증을 마쳤어요").assertExists()
        compose.onNodeWithText("체크 해제").assertExists()
        compose.onNodeWithText("오늘 완료로 체크").assertDoesNotExist()
    }

    @Test
    fun `대상일이 아니면 체크할 것이 없다고 말한다`() {
        render(state(status = TodayResultStatus.NOT_TARGET))

        compose.onNodeWithText("오늘은 인증하는 날이 아니에요", substring = true).assertExists()
    }

    @Test
    fun `점수 미반영은 체크 전에 말한다`() {
        // 누른 뒤에 알리면 점수가 오를 줄 알고 누른 사용자를 뒤늦게 배신한다.
        render(state(status = TodayResultStatus.IN_PROGRESS))

        compose.onNodeWithText("수동 인증은 점수에 반영되지 않아요").assertExists()
    }

    @Test
    fun `오늘 상태를 못 받으면 카드 대신 재시도를 준다`() {
        // 빈 카드를 그리면 "아직 인증 전"으로 읽혀 누를 수 없는 버튼만 남는다.
        render(state(status = null).copy(errorMessage = "오늘 상태를 불러오지 못했어요"))

        compose.onNodeWithText("다시 시도").assertExists()
        compose.onNodeWithText("아직 인증 전이에요").assertDoesNotExist()
    }

    private fun state(
        status: TodayResultStatus?,
        verificationId: String? = null,
    ) = ManualSubmitState
        .initial("ch-1")
        .copy(
            isLoading = false,
            title = "아침 러닝",
            date = "2026-09-14",
            window = "자정 마감",
            status = status,
            verificationId = verificationId,
            streakAfter = 9,
        )

    private fun render(state: ManualSubmitState) {
        compose.setContent {
            RuleUpTheme {
                ManualSubmitContent(state = state, onIntent = {})
            }
        }
    }
}
