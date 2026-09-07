package com.ruleup.profile.presentation.tier

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.TierBest
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.profile.domain.entity.TierSnapshot
import com.ruleup.profile.presentation.renderScreen
import com.ruleup.profile.presentation.tier.viewmodel.MyTierHistoryIntent
import com.ruleup.profile.presentation.tier.viewmodel.MyTierHistoryState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 티어 히스토리. 보관이 1년이라 **오래된 기록이 사라진다** — 그 사실을 말하지 않으면 사용자는
 * 기록이 유실됐다고 여긴다.
 *
 * 하락 사유는 정책상 표기하지 않는다 — 서버도 내려주지 않으므로 화면이 지어내면 안 된다.
 */
@RunWith(RobolectricTestRunner::class)
class MyTierHistoryContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `쌓인 기록이 없으면 빈 목록 대신 그 사실을 말한다`() {
        render(MyTierHistoryState.initial.copy(isLoading = false, history = history(monthly = emptyList())))

        compose.onNodeWithText("아직 쌓인 기록이 없어요").assertExists()
    }

    @Test
    fun `보관 기간을 함께 알려 준다`() {
        render(MyTierHistoryState.initial.copy(isLoading = false, history = history()))

        compose.onNodeWithText("1년 보관", substring = true).assertExists()
    }

    @Test
    fun `역대 최고가 없으면 지어내지 않는다`() {
        // 가입 직후에는 표본이 없다 — 0점을 최고 기록으로 세우면 없던 이력이 생긴다.
        render(MyTierHistoryState.initial.copy(isLoading = false, history = history(best = null)))

        compose.onNodeWithText("역대 최고", substring = true).assertDoesNotExist()
    }

    @Test
    fun `역대 최고가 있으면 티어와 점수를 함께 보여 준다`() {
        render(MyTierHistoryState.initial.copy(isLoading = false, history = history()))

        compose.onNodeWithText("역대 최고 골드").assertExists()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(MyTierHistoryState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    private fun history(
        best: TierBest? = TierBest(tier = Tier.GOLD, score = 420, date = "2026-05-10"),
        monthly: List<TierSnapshot> = listOf(TierSnapshot(month = "2026-06", endTier = Tier.GOLD, endScore = 302)),
    ) = TierHistory(best = best, monthly = monthly, retentionNote = "1년 보관")

    private fun render(
        state: MyTierHistoryState,
        onIntent: (MyTierHistoryIntent) -> Unit = {},
    ) {
        compose.renderScreen { MyTierHistoryContent(state = state, onIntent = onIntent) }
    }
}
