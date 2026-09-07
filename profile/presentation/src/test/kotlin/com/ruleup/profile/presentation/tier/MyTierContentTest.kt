package com.ruleup.profile.presentation.tier

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.ScoreChangeReason
import com.ruleup.profile.domain.entity.TierDemotion
import com.ruleup.profile.domain.entity.TierPromotion
import com.ruleup.profile.presentation.clickPastGuard
import com.ruleup.profile.presentation.renderScreen
import com.ruleup.profile.presentation.tier.viewmodel.MyTierIntent
import com.ruleup.profile.presentation.tier.viewmodel.MyTierState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 내 티어 (Figma 1134:1520). 점수는 사용자가 자기 위치를 확인하는 값이라 **못 불러온 것과 낮은 것을
 * 섞으면** 하지도 않은 일로 강등된 줄 안다.
 *
 * 표시 티어와 실제 티어가 갈리는 유예 구간이 이 화면의 핵심이다 — 방 입장 판정도 표시 티어를 쓴다.
 */
@RunWith(RobolectricTestRunner::class)
class MyTierContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 실패 문구를 띄우지 않는다`() {
        render(MyTierState.initial.copy(isLoading = true))

        compose.onNodeWithText("티어 정보를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(MyTierState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(MyTierState.initial.copy(isLoading = false, errorMessage = null))

        compose.onNodeWithText("티어 정보를 불러오지 못했어요").assertExists()
    }

    @Test
    fun `유예 구간에서는 실제 티어가 아니라 표시 티어를 현재로 표시한다`() {
        // 방 입장 판정이 표시 티어를 쓰므로, 화면이 실제 티어를 가리키면 들어갈 수 있는 방이 어긋난다.
        render(
            MyTierState.initial.copy(
                isLoading = false,
                tier = tier(tier = Tier.SILVER, displayTier = Tier.GOLD, score = 285, graceBand = true),
            ),
        )

        compose.onNodeWithText("골드를 지키려면").assertExists()
    }

    @Test
    fun `강등 경계는 서버가 준 점수를 그대로 말한다`() {
        render(
            MyTierState.initial.copy(
                isLoading = false,
                tier = tier(demotion = TierDemotion(graceFloor = 280, demoteAt = 279)),
            ),
        )

        compose.onNodeWithText("300점 아래로 내려가도 바로 떨어지지 않아요. 279점 이하가 되면 실버로 내려가요.").assertExists()
    }

    @Test
    fun `최상위 티어면 다음 티어까지 남은 점수를 말하지 않는다`() {
        render(
            MyTierState.initial.copy(
                isLoading = false,
                tier = tier(tier = Tier.RUBY, displayTier = Tier.RUBY, score = 1500, promotion = null),
            ),
        )

        compose.onNodeWithText("까지", substring = true).assertDoesNotExist()
    }

    @Test
    fun `점수가 움직인 적이 없으면 빈 목록 대신 그 사실을 말한다`() {
        render(MyTierState.initial.copy(isLoading = false, tier = tier(recentChanges = emptyList())))

        compose.onNodeWithText("아직 점수가 움직인 적이 없어요").assertExists()
    }

    @Test
    fun `모르는 변동 사유도 행을 지우지 않고 증감폭을 보여 준다`() {
        // 사유 enum 이 늘었다고 행이 사라지면 사용자는 점수가 왜 줄었는지 알 방법이 없다.
        render(
            MyTierState.initial.copy(
                isLoading = false,
                tier = tier(recentChanges = listOf(ScoreChange(date = "2026-08-01", reason = null, challengeId = null, delta = -3))),
            ),
        )

        compose.onNodeWithText("점수 변동").assertExists()
        compose.onNodeWithText("−3").assertExists()
    }

    @Test
    fun `전체 보기를 누르면 히스토리로 가겠다는 의도를 올린다`() {
        val intents = mutableListOf<MyTierIntent>()
        render(MyTierState.initial.copy(isLoading = false, tier = tier()), onIntent = intents::add)

        compose.onNodeWithText("전체 보기").clickPastGuard()

        assertTrue(MyTierIntent.OpenHistory in intents)
    }

    private fun tier(
        tier: Tier = Tier.GOLD,
        displayTier: Tier = Tier.GOLD,
        score: Int = 370,
        graceBand: Boolean = false,
        promotion: TierPromotion? = TierPromotion(nextTier = Tier.DIAMOND, pointsToPromote = 130),
        demotion: TierDemotion? = TierDemotion(graceFloor = 280, demoteAt = 279),
        recentChanges: List<ScoreChange> =
            listOf(
                ScoreChange(date = "2026-07-21", reason = ScoreChangeReason.CYCLE_SUCCESS, challengeId = "c1", delta = 5),
            ),
    ) = MyTier(
        tier = tier,
        score = score,
        displayTier = displayTier,
        graceBand = graceBand,
        promotion = promotion,
        demotion = demotion,
        recentChanges = recentChanges,
    )

    private fun render(
        state: MyTierState,
        onIntent: (MyTierIntent) -> Unit = {},
    ) {
        compose.renderScreen { MyTierContent(state = state, onIntent = onIntent) }
    }
}
