package com.ruleup.profile.presentation.stats

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.profile.domain.entity.CycleResult
import com.ruleup.profile.domain.entity.CycleWeek
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.entity.StatsStreak
import com.ruleup.profile.presentation.renderScreen
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsIntent
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 통계 리포트. 지표가 **표본 없음**일 때가 이 화면의 함정이다 — 0% 로 그리면 아무것도 안 한 사람과
 * 전부 실패한 사람이 같은 화면을 본다.
 */
@RunWith(RobolectricTestRunner::class)
class MyStatsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 실패 문구를 띄우지 않는다`() {
        render(MyStatsState.initial.copy(isLoading = true))

        compose.onNodeWithText("통계를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(MyStatsState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `판정된 인증이 없으면 0퍼센트 대신 그 사실을 말한다`() {
        render(MyStatsState.initial.copy(isLoading = false, report = report(successRate = null)))

        compose.onNodeWithText("아직 판정된 인증이 없어요").assertExists()
    }

    @Test
    fun `성공률은 퍼센트로 보여 준다`() {
        render(MyStatsState.initial.copy(isLoading = false, report = report(successRate = 0.87)))

        compose.onNodeWithText("87").assertExists()
    }

    @Test
    fun `지나간 주가 없으면 빈 그리드 대신 그 사실을 말한다`() {
        render(MyStatsState.initial.copy(isLoading = false, report = report(cycles = emptyList())))

        compose.onNodeWithText("아직 지나간 주가 없어요").assertExists()
    }

    @Test
    fun `연속 성공과 최고 연속을 함께 보여 준다`() {
        // 지금 끊겼다는 사실만 보이면 사용자는 자기 최고 기록을 잃은 줄 안다.
        render(MyStatsState.initial.copy(isLoading = false, report = report()))

        compose.onNodeWithText("연속 성공").assertExists()
        compose.onNodeWithText("최고 연속").assertExists()
    }

    private fun report(
        successRate: Double? = 0.87,
        cycles: List<CycleWeek> = listOf(CycleWeek(week = "2026-W27", result = CycleResult.SUCCESS)),
    ) = StatsReport(
        successRate = successRate,
        totalSuccessCount = 142,
        streak = StatsStreak(current = 6, best = 21),
        cycles12w = cycles,
        completedCount = 24,
        weeklyScoreDelta = 5,
    )

    private fun render(
        state: MyStatsState,
        onIntent: (MyStatsIntent) -> Unit = {},
    ) {
        compose.renderScreen { MyStatsContent(state = state, onIntent = onIntent) }
    }
}
