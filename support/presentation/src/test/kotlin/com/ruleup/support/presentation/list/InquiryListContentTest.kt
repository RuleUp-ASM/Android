package com.ruleup.support.presentation.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.domain.fake.inquirySummary
import com.ruleup.support.presentation.list.viewmodel.InquiryListState
import com.ruleup.support.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 내 문의 내역 (Figma `1419:27`).
 *
 * 「새 답변」 점이 이 화면의 핵심이다 — 답변을 푸시로도 알림함으로도 알리지 않으므로, 이 표시가
 * 없으면 사용자는 답변이 온 줄 모른다.
 */
@RunWith(RobolectricTestRunner::class)
class InquiryListContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `답변이 온 문의에는 새 답변 표시가 붙는다`() {
        render(
            listOf(
                inquirySummary(
                    inquiryId = "a",
                    status = InquiryStatus.ANSWERED,
                    preview = "기상 인증이 실패로 떴어요",
                    answeredAt = "2026-09-06T11:08:00Z",
                ),
            ),
        )

        compose.onNodeWithText("새 답변").assertIsDisplayed()
        compose.onNodeWithText("답변 완료").assertIsDisplayed()
    }

    @Test
    fun `아직 답변 전이면 상태 칩도 새 답변도 없다`() {
        // 「접수됨」 칩을 굳이 그리지 않는다 — 모든 줄에 붙어 새 답변만 눈에 띄어야 할 자리를 흐린다.
        render(listOf(inquirySummary(preview = "헬스 커넥트 연결이 자꾸 끊겨요")))

        compose.onNodeWithText("헬스 커넥트 연결이 자꾸 끊겨요").assertIsDisplayed()
        compose.onNodeWithText("새 답변").assertDoesNotExist()
        compose.onNodeWithText("답변 완료").assertDoesNotExist()
    }

    @Test
    fun `접수번호는 줄여서 보여 준다`() {
        // 서버가 주는 UUID 는 36자라 접수일과 같은 줄에 통째로 넣으면 날짜가 밀려 나간다.
        render(listOf(inquirySummary(inquiryId = "3d6ad414-5fb6-81aa-8d11-ca6ffd272529")))

        compose.onNodeWithText("09.05 접수 · 3d6ad414-5fb6-81aa…").assertIsDisplayed()
    }

    @Test
    fun `문의가 없으면 빈 화면 대신 그 사실을 말한다`() {
        render(emptyList())

        compose.onNodeWithText("아직 접수한 문의가 없어요").assertIsDisplayed()
    }

    @Test
    fun `목록이 비어도 문의하기 진입은 남는다`() {
        render(emptyList())

        compose.onNodeWithText("문의하기").assertIsDisplayed()
    }

    private fun render(items: List<com.ruleup.support.domain.entity.InquirySummary>) {
        compose.renderScreen {
            InquiryListContent(
                state = InquiryListState.initial.copy(isLoading = false, items = items),
                onIntent = {},
            )
        }
    }
}
