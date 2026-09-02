package com.ruleup.profile.presentation.stats

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.profile.domain.entity.StatsPeriod
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.presentation.clickPastGuard
import com.ruleup.profile.presentation.renderScreen
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsIntent
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 통계 리포트. 기간 탭이 이 화면의 본체라 **어느 기간을 보고 있는지**가 분명해야 하고,
 * 탭을 누르면 그 기간이 의도로 올라가야 한다 — 안 올라가면 화면이 이전 기간 숫자를 계속 보여 준다.
 */
@RunWith(RobolectricTestRunner::class)
class MyStatsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `기간 탭 세 종을 모두 보여 준다`() {
        render(MyStatsState.initial)

        StatsPeriod.entries.forEach { compose.onNodeWithText(it.label).assertExists() }
    }

    @Test
    fun `불러오는 중에는 실패 문구를 띄우지 않는다`() {
        render(MyStatsState.initial.copy(isLoading = true))

        compose.onNodeWithText("통계를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(MyStatsState.initial.copy(isLoading = false, errorMessage = "집계에 실패했어요"))

        compose.onNodeWithText("집계에 실패했어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(MyStatsState.initial.copy(isLoading = false, errorMessage = null))

        compose.onNodeWithText("통계를 불러오지 못했어요").assertExists()
    }

    @Test
    fun `기간 탭을 누르면 그 기간이 의도로 올라간다`() {
        // 안 올라가면 화면이 이전 기간 숫자를 계속 보여 준다.
        val intents = mutableListOf<MyStatsIntent>()
        render(MyStatsState.initial) { intents += it }

        compose.onNodeWithText(StatsPeriod.WEEKLY.label).clickPastGuard()

        assertTrue(intents.contains(MyStatsIntent.SelectPeriod(StatsPeriod.WEEKLY)))
    }

    @Test
    fun `리포트를 받으면 실패 문구를 띄우지 않는다`() {
        render(MyStatsState.initial.copy(isLoading = false, report = report()))

        compose.onNodeWithText("통계를 불러오지 못했어요").assertDoesNotExist()
    }

    private fun report() =
        StatsReport(
            period = StatsPeriod.MONTHLY,
            totalCompleted = 3,
            avgCompletionRate = 72,
            mannerDelta = 1.2,
            avgStreak = 4.0,
            series = emptyList(),
            insight = null,
        )

    private fun render(
        state: MyStatsState,
        onIntent: (MyStatsIntent) -> Unit = {},
    ) {
        compose.renderScreen { MyStatsContent(state = state, onIntent = onIntent) }
    }
}
