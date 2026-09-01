package com.ruleup.challenge.presentation.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.JoinBlock
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 가입이 막혔을 때 상세 화면이 **무엇을 말하고 무엇을 말하지 않는지**를 지킨다.
 *
 * 사유별 문구는 화면 안에서만 갈라지므로(도메인은 사유 enum 까지만 안다) 이 층이 유일한 검증 자리다.
 * 사유가 늘어나면 [expectedTitle] 의 `when` 이 컴파일되지 않는다 — 그게 열거를 끝냈다는 근거다.
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeDetailJoinBlockTest {
    @get:Rule
    val compose = createComposeRule()

    private val intents = mutableListOf<ChallengeDetailIntent>()

    @Before
    fun advanceClockBeforeEachTest() {
        advanceClockPastPreviousTests()
    }

    @Test
    fun `가입이 막힌 모든 사유는 무엇에 막혔는지 말한다`() {
        // 사유를 말하지 않는 시트는 "내가 뭘 잘못했나"만 남긴다. 사유가 늘 때 하나가 조용히 빠지는 걸 막는다.
        var reason by mutableStateOf<JoinBlockReason?>(null)
        renderBlocked { JoinBlock(reason = reason) }

        JoinBlockReason.entries.forEach { entry ->
            compose.runOnIdle { reason = entry }

            compose.onNodeWithText(expectedTitle(entry)).assertIsDisplayed()
        }
    }

    @Test
    fun `앱이 모르는 사유여도 빈 시트를 띄우지 않는다`() {
        // 서버가 사유를 추가하면 구버전 앱에는 null 로 온다. 그때 아무 말 없는 시트가 뜨면 버그로 읽힌다.
        renderBlocked { JoinBlock(reason = null) }

        compose.onNodeWithText("지금은 참여할 수 없어요").assertIsDisplayed()
    }

    @Test
    fun `티어로 막혔으면 필요한 티어와 내 티어를 같이 보여준다`() {
        // 필요한 티어만 보여주면 얼마나 모자란지 모른다. 비교 기준은 표시 티어다.
        renderBlocked(
            detail = challengeDetail(minTier = Tier.GOLD, myDisplayTier = Tier.SILVER),
        ) { JoinBlock(reason = JoinBlockReason.TIER_GATE) }

        compose.onNodeWithText("필요한 티어 GOLD · 내 티어 SILVER").assertIsDisplayed()
    }

    @Test
    fun `티어를 모르면 티어를 지어내지 않는다`() {
        renderBlocked { JoinBlock(reason = JoinBlockReason.TIER_GATE) }

        compose.onNodeWithText("필요한 티어 - · 내 티어 -").assertIsDisplayed()
    }

    @Test
    fun `차단된 사용자에게 차단 사유를 설명하지 않는다`() {
        // 사유를 알리면 우회 시도를 부르고, 다음 행동을 주면 계속 두드리게 된다. 둘 다 하지 않는 게 정책이다.
        renderBlocked { JoinBlock(reason = JoinBlockReason.BANNED) }

        compose.onNodeWithText("자세한 내용은 안내드릴 수 없어요").assertIsDisplayed()
        compose.onNodeWithText("참여 중인 챌린지 보기").assertDoesNotExist()
        compose.onNodeWithText("내 티어 보기").assertDoesNotExist()
        compose.onNodeWithText("다른 챌린지 찾기").assertDoesNotExist()
    }

    @Test
    fun `재입장 가능일을 알면 언제부터인지 말한다`() {
        renderBlocked {
            JoinBlock(
                reason = JoinBlockReason.REJOIN_COOLDOWN,
                rejoinAvailableAt = "2026-09-15T00:00:00+09:00",
            )
        }

        compose.onNodeWithText("2026-09-15 부터 다시 참여할 수 있어요").assertIsDisplayed()
    }

    @Test
    fun `재입장 가능일을 모르면 날짜를 지어내지 않는다`() {
        // 탈퇴 1주·강퇴 배수라 남은 기간을 앱이 계산할 수 없다. 없는 날짜를 그리면 그날 와도 또 막힌다.
        renderBlocked { JoinBlock(reason = JoinBlockReason.REJOIN_COOLDOWN, rejoinAvailableAt = null) }

        compose.onNodeWithText("조금 뒤에 다시 시도해 주세요").assertIsDisplayed()
    }

    @Test
    fun `동시 참여 수에 막혔으면 정리할 수 있는 곳으로 보낸다`() {
        renderBlocked { JoinBlock(reason = JoinBlockReason.FREE_LIMIT) }

        compose.onNodeWithText("참여 중인 챌린지 보기").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.FollowJoinBlockAction), intents)
    }

    @Test
    fun `안내를 닫으면 차단 안내를 내린다`() {
        renderBlocked { JoinBlock(reason = JoinBlockReason.FULL) }

        compose.onNodeWithText("닫기").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.DismissJoinBlock), intents)
    }

    /**
     * 사유 → 제목. 프로덕션과 같은 문구를 여기 한 번 더 적는 대신, `else` 없는 `when` 으로 **사유가
     * 늘면 컴파일이 깨지게** 만든다 — 문구가 바뀌면 이 테스트가 빨개지는 건 의도한 비용이다.
     */
    private fun expectedTitle(reason: JoinBlockReason): String =
        when (reason) {
            JoinBlockReason.REJOIN_COOLDOWN -> "아직 다시 들어올 수 없어요"
            JoinBlockReason.FREE_LIMIT -> "동시에 3개까지 참여할 수 있어요"
            JoinBlockReason.FULL -> "정원이 찼어요"
            JoinBlockReason.TIER_GATE -> "티어 조건을 만족하지 않아요"
            JoinBlockReason.BANNED -> "이 챌린지에는 참여할 수 없어요"
            JoinBlockReason.CHALLENGE_COMPLETED -> "이미 끝난 챌린지예요"
            // 안내할 게 따로 없는 사유는 일반 문구로 떨어진다.
            JoinBlockReason.PRIVATE_INVITE_ONLY, JoinBlockReason.ALREADY_JOINED -> "지금은 참여할 수 없어요"
        }

    private fun renderBlocked(
        detail: ChallengeDetail = challengeDetail(),
        block: () -> JoinBlock,
    ) {
        compose.setContent {
            RuleUpTheme {
                ChallengeDetailContent(
                    state = detailState(detail = detail, joinBlock = block()),
                    ctaLabel = "참여하기",
                    onIntent = { intents += it },
                    onBack = {},
                    onCta = {},
                )
            }
        }
    }
}
