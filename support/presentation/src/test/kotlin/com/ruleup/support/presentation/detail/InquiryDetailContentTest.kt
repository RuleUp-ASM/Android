package com.ruleup.support.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.support.domain.fake.inquiryDetail
import com.ruleup.support.presentation.detail.viewmodel.InquiryDetailState
import com.ruleup.support.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 문의 상세. 기대값의 출처는 Figma `1419:87` 이되 **두 곳은 일부러 다르게 간다** — 재문의 안내와
 * 알림함 문구는 계약이 사라졌거나(재문의 폐지) 쓰지 않기로 한(알림) 것이라, 그대로 두면 화면이
 * 없는 경로를 약속하게 된다.
 */
@RunWith(RobolectricTestRunner::class)
class InquiryDetailContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `답변이 오면 운영팀 답변을 본문과 나눠 보여 준다`() {
        render(inquiryDetail(answerText = "확인 결과 기상 신호가 허용 범위보다 이르게 수집됐습니다."))

        compose.onNodeWithText("룰업 운영팀 답변").assertIsDisplayed()
        compose.onNodeWithText("확인 결과 기상 신호가 허용 범위보다 이르게 수집됐습니다.").assertIsDisplayed()
    }

    @Test
    fun `아직 답변 전이면 빈 자리 대신 기다리는 중이라고 말한다`() {
        // 답변 영역이 비어 있으면 로딩 실패로 읽혀서 사용자가 같은 문의를 다시 넣는다.
        render(inquiryDetail(answerText = null))

        compose.onNodeWithText("아직 답변 전이에요. 영업일 2일 안에 답변드릴게요.").assertIsDisplayed()
    }

    @Test
    fun `재문의 기한을 안내하지 않는다`() {
        // 명세에서 재문의가 폐지됐다. 「7일 안에 한 번 더」를 남기면 입력창을 찾다 시간을 버린다.
        render(inquiryDetail(answerText = "확인했습니다."))

        compose.onNodeWithText("답변 후 7일 안에 한 번 더 질문할 수 있어요").assertDoesNotExist()
    }

    @Test
    fun `답변을 알림함으로 알린다고 말하지 않는다`() {
        // 알림을 쓰지 않기로 했다. 알린다고 해 두면 사용자가 알림함만 보다 답변을 놓친다.
        render(inquiryDetail(answerText = "확인했습니다."))

        compose.onNodeWithText("답변이 오면 알림함으로 알려드려요").assertDoesNotExist()
    }

    @Test
    fun `다시 물으려면 새 문의라는 걸 버튼이 말한다`() {
        render(inquiryDetail(answerText = "확인했습니다."))

        compose.onNodeWithText("새 문의하기").assertIsDisplayed()
    }

    private fun render(detail: com.ruleup.support.domain.entity.InquiryDetail) {
        compose.renderScreen {
            InquiryDetailContent(
                state = InquiryDetailState.initial.copy(isLoading = false, detail = detail),
                onIntent = {},
            )
        }
    }
}
