package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ChallengeCalendar
import com.ruleup.challenge.domain.entity.ChallengeCalendarDay
import com.ruleup.challenge.domain.entity.ChallengeDayStatus
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.detail.component.SoloMonthCalendar
import com.ruleup.challenge.presentation.detail.component.monthTitle
import com.ruleup.challenge.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 솔로 상세의 월 캘린더 (Figma 1134:1930).
 *
 * 색은 검증하지 않는다(디자인 토큰이 코드로 이미 말한다). 여기서 지키는 건 **달의 모양**이다 —
 * 물어본 달이 그대로 보이고, 그 달의 날짜가 빠짐없이 서고, 유예 창이 별도 범례를 갖는 것.
 */
@RunWith(RobolectricTestRunner::class)
class SoloMonthCalendarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `보고 있는 달을 제목에 그대로 쓴다`() {
        render(month = "2026-09")

        compose.onNodeWithText("2026년 9월").assertExists()
    }

    @Test
    fun `그 달의 날짜를 빠짐없이 세운다`() {
        // 9월은 30일이다. 한 칸이라도 빠지면 사용자가 자기 기록을 못 찾는다.
        render(month = "2026-09")

        compose.onAllNodesWithText("30").assertCountEquals(1)
        compose.onAllNodesWithText("1").assertCountEquals(1)
    }

    @Test
    fun `유예 창은 실패와 다른 범례를 갖는다`() {
        // 아직 뒤집힐 수 있는 날이 확정 실패와 같아 보이면 이의를 낼 수 있는데도 포기하게 된다.
        render(month = "2026-09")

        compose.onNodeWithText("실패 예정").assertExists()
        compose.onNodeWithText("실패").assertExists()
    }

    @Test
    fun `이전 달을 누르면 월 이동 의도를 올린다`() {
        val offsets = mutableListOf<Long>()
        render(month = "2026-09", onPrevMonth = { offsets += -1 })

        compose.onNodeWithText("◀").clickPastGuard()

        assertEquals(listOf(-1L), offsets)
    }

    @Test
    fun `읽는 중이면 그 사실을 말한다`() {
        // 빈 달과 아직 못 받은 달이 같아 보이면 "기록이 사라졌다"로 읽힌다.
        render(month = "2026-09", isLoading = true)

        compose.onNodeWithText("불러오는 중…").assertExists()
    }

    @Test
    fun `파싱할 수 없는 달은 지어내지 않고 원문을 남긴다`() {
        assertTrue(monthTitle("2026-13") == "2026-13")
    }

    private fun render(
        month: String,
        calendar: ChallengeCalendar? = calendar(month),
        isLoading: Boolean = false,
        onPrevMonth: () -> Unit = {},
        onNextMonth: () -> Unit = {},
    ) {
        compose.renderScreen {
            SoloMonthCalendar(
                month = month,
                calendar = calendar,
                isLoading = isLoading,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                today = LocalDate.parse("$month-15"),
            )
        }
    }

    private fun calendar(month: String) =
        ChallengeCalendar(
            challengeId = "c1",
            month = month,
            days =
                listOf(
                    day("$month-01", ChallengeDayStatus.DONE),
                    day("$month-03", ChallengeDayStatus.FAILED),
                    day("$month-05", ChallengeDayStatus.CHECKING),
                ),
        )

    private fun day(
        date: String,
        status: ChallengeDayStatus,
    ) = ChallengeCalendarDay(date = date, status = status, verificationId = null, appealable = false)
}
