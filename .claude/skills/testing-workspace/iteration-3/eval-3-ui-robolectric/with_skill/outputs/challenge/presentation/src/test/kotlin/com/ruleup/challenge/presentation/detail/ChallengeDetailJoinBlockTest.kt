package com.ruleup.challenge.presentation.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.JoinBlock
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 참여가 막혔을 때 띄우는 안내 시트 (명세 409 `JOIN_BLOCKED`).
 *
 * 기준은 Figma 참여 차단 시트 3종(`1134:1077`) — 정원·티어·무료 한도. 나머지 사유는 프레임이 없어
 * 문구를 지어내지 않고 **"사유를 밝히지 않는다"·"닫을 길이 있다"** 처럼 정책이 정한 것만 못 박는다.
 *
 * 디자인과 코드가 어긋나 이 파일이 판정하지 않는 두 건은 `TEST_STRATEGY.md` 미결 목록에 있다:
 * 정원(FULL)의 다음 행동 유무, 무료 한도(FREE_LIMIT)의 버튼 문구. 테스트로 한쪽을 못 박으면
 * 그게 정답이 되어 버린다.
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeDetailJoinBlockTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun passGlobalClickGuard() {
        advanceClockPastGuard()
    }

    @Test
    fun `앱이 모르는 사유를 포함해 모든 차단 사유가 닫을 길을 남긴다`() {
        // 서버가 사유를 늘려도 시트에 갇히면 안 된다. `entries` 를 도니 값이 늘면 여기가 먼저 깨진다.
        var block by mutableStateOf(JoinBlock(reason = null))
        compose.setContent {
            RuleUpTheme {
                ChallengeDetailContent(
                    state = state(joinBlock = block),
                    ctaLabel = "참여하기",
                    onIntent = {},
                    onBack = {},
                    onCta = {},
                )
            }
        }

        (JoinBlockReason.entries.map { JoinBlock(it) } + JoinBlock(reason = null)).forEach { next ->
            compose.runOnIdle { block = next }
            compose.awaitText("닫기")
            compose.onNodeWithText("닫기").assertIsDisplayed()
        }
    }

    @Test
    fun `티어에 막히면 필요한 티어와 내 티어를 함께 보여준다`() {
        // 둘 중 하나만 보이면 얼마나 모자란지 모른 채 시트를 닫는다.
        // Figma 1134:1088 도 제목에 필요한 티어를, 본문에 내 티어를 함께 적는다.
        compose.showDetail(
            state(
                detail = detail(minTier = Tier.SILVER, myDisplayTier = Tier.BRONZE, eligible = false),
                joinBlock = JoinBlock(JoinBlockReason.TIER_GATE),
            ),
        )
        compose.awaitText("닫기")

        compose.onAllNodesWithText("SILVER", substring = true).assertCountEquals(1)
        compose.onAllNodesWithText("BRONZE", substring = true).assertCountEquals(1)
        // Figma 1134:1095 의 버튼 문구.
        compose.onNodeWithText("내 티어 보기").assertIsDisplayed()
    }

    @Test
    fun `티어 안내의 다음 행동을 누르면 그 행동 의도가 올라간다`() {
        val intents = mutableListOf<ChallengeDetailIntent>()
        compose.showDetail(
            state(
                detail = detail(minTier = Tier.SILVER, eligible = false),
                joinBlock = JoinBlock(JoinBlockReason.TIER_GATE),
            ),
            onIntent = { intents += it },
        )
        compose.awaitText("내 티어 보기")

        compose.onNodeWithText("내 티어 보기").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.FollowJoinBlockAction), intents)
    }

    @Test
    fun `재입장 대기는 스스로 나갔는지 강퇴됐는지 구분해 말하지 않는다`() {
        // 같은 대기라도 원인을 밝히면 방장이 누구를 내보냈는지가 드러나고, 당사자에게는
        // 낙인으로 남는다. 안내에 필요한 건 "언제부터 되는가" 하나다(챌린지 정책 §7).
        compose.showDetail(
            state(joinBlock = JoinBlock(JoinBlockReason.REJOIN_COOLDOWN, rejoinAvailableAt = "2026-09-07T00:00:00+09:00")),
        )
        compose.awaitText("닫기")

        compose.onAllNodesWithText("강퇴", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("탈퇴", substring = true).assertCountEquals(0)
        compose.onNodeWithText("2026-09-07", substring = true).assertIsDisplayed()
    }

    @Test
    fun `영구 차단은 차단된 이유를 설명하지 않는다`() {
        // 사유를 적으면 어떤 행동이 걸렸는지 역추적할 수 있어 우회 방법을 알려주는 셈이 된다.
        compose.showDetail(state(joinBlock = JoinBlock(JoinBlockReason.BANNED)))
        compose.awaitText("닫기")

        compose.onAllNodesWithText("신고", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("위반", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("제재", substring = true).assertCountEquals(0)
    }

    @Test
    fun `차단 시트의 닫기를 누르면 시트 해제 의도가 올라간다`() {
        val intents = mutableListOf<ChallengeDetailIntent>()
        compose.showDetail(
            state(joinBlock = JoinBlock(JoinBlockReason.FULL)),
            onIntent = { intents += it },
        )
        compose.awaitText("닫기")

        compose.onNodeWithText("닫기").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.DismissJoinBlock), intents)
    }
}
