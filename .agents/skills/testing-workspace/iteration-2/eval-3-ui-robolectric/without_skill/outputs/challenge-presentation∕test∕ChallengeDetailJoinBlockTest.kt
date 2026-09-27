package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.JoinBlock
import com.ruleup.domain.entity.user.Tier
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * 가입 차단 안내 시트. 사유마다 **문구와 다음 행동이 다르다** — 여기가 어긋나면 사용자는 자기가 왜
 * 막혔는지 모른 채 같은 버튼을 다시 누른다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChallengeDetailJoinBlockTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `재입장 대기는 언제부터 가능한지를 말한다`() {
        show(JoinBlock(reason = JoinBlockReason.REJOIN_COOLDOWN, rejoinAvailableAt = "2026-09-08T00:00:00+09:00"))

        compose.awaitText("아직 다시 들어올 수 없어요")
        compose.onNodeWithText("2026-09-08 부터 다시 참여할 수 있어요").assertExists()
    }

    @Test
    fun `재입장 시각을 모르면 날짜를 지어내지 않는다`() {
        show(JoinBlock(reason = JoinBlockReason.REJOIN_COOLDOWN, rejoinAvailableAt = null))

        compose.awaitText("조금 뒤에 다시 시도해 주세요")
    }

    @Test
    fun `탈퇴인지 강퇴인지는 밝히지 않는다`() {
        // 둘을 구분하는 문구는 알려서 얻는 것보다 잃는 게 크다.
        show(JoinBlock(reason = JoinBlockReason.REJOIN_COOLDOWN, rejoinAvailableAt = null))

        compose.awaitText("아직 다시 들어올 수 없어요")
        compose.onNodeWithText("강퇴", substring = true).assertDoesNotExist()
        compose.onNodeWithText("탈퇴", substring = true).assertDoesNotExist()
    }

    @Test
    fun `동시 참여 한도는 정리할 방으로 보낸다`() {
        val harness = show(JoinBlock(reason = JoinBlockReason.FREE_LIMIT))

        compose.awaitText("동시에 3개까지 참여할 수 있어요")
        compose.clickAwaited("참여 중인 챌린지 보기")

        harness.last<ChallengeDetailIntent.FollowJoinBlockAction>()
    }

    @Test
    fun `티어 게이트는 필요한 티어와 내 티어를 나란히 놓는다`() {
        val harness =
            show(
                block = JoinBlock(reason = JoinBlockReason.TIER_GATE),
                challenge = detail(minTier = Tier.GOLD, myDisplayTier = Tier.BRONZE),
            )

        compose.awaitText("티어 조건을 만족하지 않아요")
        compose.onNodeWithText("필요한 티어 GOLD · 내 티어 BRONZE").assertExists()
        compose.clickAwaited("내 티어 보기")

        harness.last<ChallengeDetailIntent.FollowJoinBlockAction>()
    }

    @Test
    fun `티어를 모르면 자리만 비운다`() {
        show(
            block = JoinBlock(reason = JoinBlockReason.TIER_GATE),
            challenge = detail(minTier = null, myDisplayTier = null),
        )

        compose.awaitText("필요한 티어 - · 내 티어 -")
    }

    @Test
    fun `정원이 찼을 때는 다음 행동을 만들지 않는다`() {
        // 자리가 나기를 기다리는 것 말고 사용자가 할 수 있는 일이 없다.
        show(JoinBlock(reason = JoinBlockReason.FULL))

        compose.awaitText("정원이 찼어요")
        compose.onNodeWithText("자리가 나면 다시 참여할 수 있어요").assertExists()
        compose.onNodeWithText("참여 중인 챌린지 보기").assertDoesNotExist()
        compose.onNodeWithText("다른 챌린지 찾기").assertDoesNotExist()
    }

    @Test
    fun `차단 사유는 설명하지 않는다`() {
        show(JoinBlock(reason = JoinBlockReason.BANNED))

        compose.awaitText("이 챌린지에는 참여할 수 없어요")
        compose.onNodeWithText("자세한 내용은 안내드릴 수 없어요").assertExists()
    }

    @Test
    fun `끝난 챌린지는 대체를 찾게 해 준다`() {
        val harness = show(JoinBlock(reason = JoinBlockReason.CHALLENGE_COMPLETED))

        compose.awaitText("이미 끝난 챌린지예요")
        compose.clickAwaited("다른 챌린지 찾기")

        harness.last<ChallengeDetailIntent.FollowJoinBlockAction>()
    }

    @Test
    fun `앱이 모르는 사유는 일반 안내로 떨어뜨린다`() {
        // 서버가 사유를 추가해도 빈 시트가 뜨면 안 된다.
        show(JoinBlock(reason = null))

        compose.awaitText("지금은 참여할 수 없어요")
        compose.onNodeWithText("잠시 후 다시 시도해 주세요").assertExists()
    }

    @Test
    fun `닫으면 시트를 접는다`() {
        val harness = show(JoinBlock(reason = JoinBlockReason.FULL))

        compose.clickAwaited("닫기")

        harness.last<ChallengeDetailIntent.DismissJoinBlock>()
    }

    private fun show(
        block: JoinBlock,
        challenge: ChallengeDetail = detail(),
    ): DetailHarness = compose.showDetail(loadedState(challenge).copy(joinBlock = block))
}
