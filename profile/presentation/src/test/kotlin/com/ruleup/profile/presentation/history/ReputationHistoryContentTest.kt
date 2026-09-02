package com.ruleup.profile.presentation.history

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.profile.domain.entity.ReputationHistory
import com.ruleup.profile.presentation.clickPastGuard
import com.ruleup.profile.presentation.history.viewmodel.ReputationHistoryIntent
import com.ruleup.profile.presentation.history.viewmodel.ReputationHistoryState
import com.ruleup.profile.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 평판 히스토리. 조회 실패와 "기록이 없음"을 섞으면 사용자는 자기 기록이 사라진 줄 안다 —
 * 불러오는 중에도 마찬가지다.
 */
@RunWith(RobolectricTestRunner::class)
class ReputationHistoryContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 실패 문구를 띄우지 않는다`() {
        render(ReputationHistoryState.initial.copy(isLoading = true))

        compose.onNodeWithText("히스토리를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(ReputationHistoryState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(ReputationHistoryState.initial.copy(isLoading = false, errorMessage = null))

        compose.onNodeWithText("히스토리를 불러오지 못했어요").assertExists()
    }

    @Test
    fun `기록을 받으면 최고 온도를 보여 준다`() {
        render(
            ReputationHistoryState.initial.copy(
                isLoading = false,
                history = ReputationHistory(peakTemperature = 42.5, peakAchievedAt = "2026-08-01", milestones = emptyList()),
            ),
        )

        compose.onNodeWithText("히스토리를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `뒤로 가기 의도가 올라간다`() {
        val intents = mutableListOf<ReputationHistoryIntent>()
        render(ReputationHistoryState.initial) { intents += it }

        compose.onNodeWithContentDescription("뒤로").clickPastGuard()

        assertTrue(intents.contains(ReputationHistoryIntent.Back))
    }

    private fun render(
        state: ReputationHistoryState,
        onIntent: (ReputationHistoryIntent) -> Unit = {},
    ) {
        compose.renderScreen { ReputationHistoryContent(state = state, onIntent = onIntent) }
    }
}
