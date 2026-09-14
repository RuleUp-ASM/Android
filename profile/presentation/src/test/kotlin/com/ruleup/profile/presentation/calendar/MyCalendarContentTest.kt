package com.ruleup.profile.presentation.calendar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarIntent
import com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarState
import com.ruleup.profile.presentation.clickPastGuard
import com.ruleup.profile.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 활동 캘린더. 월을 오가는 화면이라 **어느 달을 보고 있는지 정해지기 전에는 달력을 그리지 않는다** —
 * 빈 달력을 먼저 그리면 사용자는 그 달에 기록이 없다고 읽는다.
 */
@RunWith(RobolectricTestRunner::class)
class MyCalendarContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `보고 있는 달이 정해지기 전에는 달력을 그리지 않는다`() {
        // 빈 달력을 먼저 그리면 그 달에 기록이 없다고 읽힌다.
        render(MyCalendarState.initial)

        compose.onNodeWithText("활동 캘린더").assertExists()
    }

    @Test
    fun `달이 정해지면 그 달을 보여 준다`() {
        render(MyCalendarState.initial.copy(month = "2026-09"))

        compose.onNodeWithText("활동 캘린더").assertExists()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 줘 기록 없는 달과 구분된다`() {
        // 사유를 안 그리면 실패한 달이 "그 달에 기록이 없다"로 읽힌다(#403).
        render(MyCalendarState.initial.copy(month = "2026-09", errorMessage = "캘린더를 못 불러왔어요"))

        compose.onNodeWithText("캘린더를 못 불러왔어요").assertExists()
    }

    @Test
    fun `조회 실패 뒤 다시 시도 의도가 올라간다`() {
        val intents = mutableListOf<MyCalendarIntent>()
        render(MyCalendarState.initial.copy(month = "2026-09", errorMessage = "캘린더를 못 불러왔어요")) { intents += it }

        compose.onNodeWithText("다시 시도").clickPastGuard()

        assertTrue(MyCalendarIntent.Retry in intents)
    }

    @Test
    fun `뒤로 가기 의도가 올라간다`() {
        val intents = mutableListOf<MyCalendarIntent>()
        render(MyCalendarState.initial) { intents += it }

        compose.onNodeWithContentDescription("뒤로").clickPastGuard()

        assertTrue(intents.contains(MyCalendarIntent.Back))
    }

    private fun render(
        state: MyCalendarState,
        onIntent: (MyCalendarIntent) -> Unit = {},
    ) {
        compose.renderScreen { MyCalendarContent(state = state, onIntent = onIntent) }
    }
}
