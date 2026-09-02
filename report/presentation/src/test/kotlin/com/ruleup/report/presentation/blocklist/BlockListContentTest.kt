package com.ruleup.report.presentation.blocklist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.BlockList
import com.ruleup.report.presentation.blocklist.fake.blockedChallenge
import com.ruleup.report.presentation.blocklist.fake.blockedUser
import com.ruleup.report.presentation.blocklist.fake.emptyBlocks
import com.ruleup.report.presentation.blocklist.viewmodel.BlockListIntent
import com.ruleup.report.presentation.blocklist.viewmodel.BlockListState
import com.ruleup.report.presentation.blocklist.viewmodel.BlockTarget
import com.ruleup.report.presentation.clickPastGuard
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BlockListContentTest {
    @get:Rule val compose = createComposeRule()

    private val intents = mutableListOf<BlockListIntent>()

    private fun show(state: BlockListState) {
        compose.setContent {
            RuleUpTheme {
                BlockListContent(state = state, onIntent = { intents += it })
            }
        }
    }

    private fun loaded(blocks: BlockList) = BlockListState.initial.copy(isLoading = false, blocks = blocks)

    @Test
    fun `차단한 대상이 없으면 빈 상태 문구를 보여준다`() {
        show(loaded(emptyBlocks()))

        compose.onNodeWithText("아직 차단한 대상이 없어요").assertIsDisplayed()
    }

    @Test
    fun `차단한 사용자와 챌린지를 각각 구역으로 나눠 보여준다`() {
        show(loaded(BlockList(users = listOf(blockedUser()), challenges = listOf(blockedChallenge()))))

        compose.onNodeWithText("사용자").assertIsDisplayed()
        compose.onNodeWithText("챌린지").assertIsDisplayed()
        compose.onNodeWithText("임시 이름 4f2a").assertIsDisplayed()
        compose.onNodeWithText("확인 중인 챌린지 c81d").assertIsDisplayed()
    }

    @Test
    fun `참여 중인 챌린지는 참여 중임을 함께 적는다`() {
        // 참여 중이면 탐색에서 사라지는 게 아니라 이름·이미지만 가려진다 — 결과가 다르다.
        show(loaded(BlockList(users = emptyList(), challenges = listOf(blockedChallenge(participating = true)))))

        compose.onNodeWithText("참여 중 · 8.30 차단").assertIsDisplayed()
    }

    @Test
    fun `해제해도 신고 기록이 남는다는 것을 목록에서 알린다`() {
        // 이 문장이 없으면 해제를 신고 취소로 읽는다.
        show(loaded(BlockList(users = listOf(blockedUser()), challenges = emptyList())))

        compose.onNodeWithText("차단을 풀어도 신고 기록은 남아요").assertIsDisplayed()
    }

    @Test
    fun `차단 해제를 누르면 바로 풀지 않고 확인을 요청한다`() {
        show(loaded(BlockList(users = listOf(blockedUser()), challenges = emptyList())))

        compose.onNodeWithText("차단 해제").clickPastGuard()

        val requested = intents.filterIsInstance<BlockListIntent.ConfirmUnblock>().single()
        assertEquals("u-1", requested.target.id)
        assertTrue(requested.target is BlockTarget.User)
    }

    @Test
    fun `확인 시트는 해제가 신고 취소가 아님을 문장으로 말한다`() {
        show(
            loaded(BlockList(users = listOf(blockedUser()), challenges = emptyList()))
                .copy(confirming = BlockTarget.User("u-1", "임시 이름 4f2a")),
        )

        compose.onNodeWithText("차단을 풀까요?").assertIsDisplayed()
        compose.onNodeWithText("이 사람의 글과 프로필이 다시 보여요. 신고 기록은 그대로 남아요.").assertIsDisplayed()
    }

    @Test
    fun `챌린지 확인 시트는 다시 탐색에 나타난다고 말한다`() {
        show(
            loaded(BlockList(users = emptyList(), challenges = listOf(blockedChallenge())))
                .copy(confirming = BlockTarget.Challenge("c-1", "확인 중인 챌린지")),
        )

        compose.onNodeWithText("이 챌린지가 탐색 목록에 다시 나타나요. 신고 기록은 그대로 남아요.").assertIsDisplayed()
    }

    @Test
    fun `해제 진행 중에는 진행 상태를 버튼에 드러낸다`() {
        show(
            loaded(BlockList(users = listOf(blockedUser()), challenges = emptyList()))
                .copy(confirming = BlockTarget.User("u-1", "임시 이름"), unblocking = true),
        )

        compose.onNodeWithText("푸는 중").assertIsDisplayed()
    }

    @Test
    fun `목록이 비어 있는데 조회가 실패하면 다시 시도를 권한다`() {
        show(loaded(emptyBlocks()).copy(errorMessage = "목록을 불러오지 못했어요"))

        compose.onNodeWithText("목록을 불러오지 못했어요").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").assertIsDisplayed()
    }
}
