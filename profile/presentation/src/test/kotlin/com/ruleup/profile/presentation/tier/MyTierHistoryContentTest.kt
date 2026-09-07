package com.ruleup.profile.presentation.tier

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.ScoreChangeReason
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
 * 화면은 원천이 둘이다 — 그래프(월말 스냅샷)와 점수 변동 이력. **한쪽이 없어도 다른 쪽은 그린다.**
 * 하락 사유 비표기는 그래프 한정이라(2026-09-07 개정) 이력에는 사유를 쓴다.
 */
@RunWith(RobolectricTestRunner::class)
class MyTierHistoryContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `변동 이력이 비면 빈 목록 대신 그 사실을 말한다`() {
        render(MyTierHistoryState.initial.copy(isLoading = false, history = history(), changes = emptyList()))

        compose.onNodeWithText("아직 점수가 움직인 적이 없어요").assertExists()
    }

    @Test
    fun `변동 행은 챌린지명과 사유를 함께 보여 준다`() {
        // id 만으로는 사용자가 어느 방인지 읽을 수 없다 — challengeTitle 이 그래서 신설됐다.
        render(MyTierHistoryState.initial.copy(isLoading = false, history = history(), changes = listOf(change())))

        compose.onNodeWithText("아침 6:30 기상 · 사이클 성공").assertExists()
        compose.onNodeWithText("+8").assertExists()
    }

    @Test
    fun `챌린지명이 없으면 사유만 남기고 자리를 비우지 않는다`() {
        // 완료된 방은 원본이 하드 삭제돼 서버도 이름을 못 채운다 — 정상 경로다.
        render(
            MyTierHistoryState.initial.copy(
                isLoading = false,
                history = history(),
                changes = listOf(change(challengeTitle = null)),
            ),
        )

        compose.onNodeWithText("사이클 성공").assertExists()
    }

    @Test
    fun `이력 API 의 보관 일수를 알려 준다`() {
        render(
            MyTierHistoryState.initial.copy(
                isLoading = false,
                history = history(),
                changes = listOf(change()),
                retentionDays = 365,
            ),
        )

        compose.onNodeWithText("365일 보관", substring = true).assertExists()
    }

    @Test
    fun `이력 조회가 실패해도 그래프의 보관 문구로 떨어진다`() {
        // 두 원천이 따로라 한쪽이 없다고 화면이 아무 말도 안 하면 안 된다.
        render(MyTierHistoryState.initial.copy(isLoading = false, history = history(), retentionDays = null))

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
    fun `둘 다 못 받았을 때만 오류 문구를 보여 준다`() {
        render(MyTierHistoryState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `그래프만 실패하면 이력은 그대로 그린다`() {
        render(MyTierHistoryState.initial.copy(isLoading = false, history = null, changes = listOf(change())))

        compose.onNodeWithText("아침 6:30 기상 · 사이클 성공").assertExists()
    }

    private fun change(challengeTitle: String? = "아침 6:30 기상") =
        ScoreChange(
            date = "2026-09-07",
            reason = ScoreChangeReason.CYCLE_SUCCESS,
            challengeId = "c1",
            challengeTitle = challengeTitle,
            delta = 8,
        )

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
