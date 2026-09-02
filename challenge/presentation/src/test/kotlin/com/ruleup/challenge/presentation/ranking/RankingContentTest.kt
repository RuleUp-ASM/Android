package com.ruleup.challenge.presentation.ranking

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.RankingEntry
import com.ruleup.challenge.domain.entity.RoomUser
import com.ruleup.challenge.presentation.ranking.viewmodel.RankingIntent
import com.ruleup.challenge.presentation.ranking.viewmodel.RankingState
import com.ruleup.challenge.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 그룹 랭킹. **미등재(10회 미만)를 꼴찌로 보이게 하면 안 된다** — 아직 자격이 안 된 것과
 * 못 한 것은 다르고, 사용자는 후자로 읽는다.
 */
@RunWith(RobolectricTestRunner::class)
class RankingContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 실패 문구를 띄우지 않는다`() {
        render(RankingState.initial.copy(isLoading = true))

        compose.onNodeWithText("랭킹을 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(RankingState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(RankingState.initial.copy(isLoading = false, errorMessage = null))

        compose.onNodeWithText("랭킹을 불러오지 못했어요").assertExists()
    }

    @Test
    fun `아직 등재되지 않았으면 순위 없음이라고 말한다`() {
        // 0등으로 접으면 꼴찌로 보인다 — 아직 자격이 안 된 것과 못 한 것은 다르다.
        render(loaded(me = unranked()))

        compose.onNodeWithText("아직 순위가 없어요").assertExists()
    }

    @Test
    fun `등재됐으면 내 순위를 보여 준다`() {
        render(loaded(me = ranked(3)))

        compose.onNodeWithText("내 순위 #3").assertExists()
        compose.onNodeWithText("아직 순위가 없어요").assertDoesNotExist()
    }

    @Test
    fun `참여자가 아직 없으면 집계 전임을 알린다`() {
        // 빈 목록을 그냥 두면 사용자는 화면이 고장 난 줄 안다.
        render(loaded(me = unranked(), entries = emptyList()))

        compose.onNodeWithText("아직 랭킹이 집계되지 않았어요").assertExists()
    }

    private fun loaded(
        me: MyRank,
        entries: List<RankingEntry> = listOf(entry("u1", 1)),
    ) = RankingState.initial.copy(
        isLoading = false,
        ranking = ChallengeRanking(me = me, items = entries),
    )

    private fun ranked(rank: Int) = MyRank(rank = rank, ranked = true, successRate = 0.9, participations = 12, gapToFirst = 0.05)

    private fun unranked() = MyRank(rank = null, ranked = false, successRate = null, participations = 3, gapToFirst = null)

    private fun entry(
        userId: String,
        rank: Int,
    ) = RankingEntry(
        rank = rank,
        user = RoomUser(userId = userId, nickname = userId, profileImageUrl = null, blocked = false),
        successRate = 0.9,
        successCount = 9,
        participations = 10,
    )

    private fun render(
        state: RankingState,
        onIntent: (RankingIntent) -> Unit = {},
    ) {
        compose.renderScreen { RankingContent(state = state, onIntent = onIntent) }
    }
}
