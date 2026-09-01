package com.ruleup.home.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.test.ClickClock
import com.ruleup.home.presentation.viewmodel.HomeFilter
import com.ruleup.home.presentation.viewmodel.HomeIntent
import com.ruleup.home.presentation.viewmodel.HomeState
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.ui.helper.LocalObservability
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import kotlin.test.assertTrue

/**
 * 홈. 첫 화면이라 **빈 상태를 어떻게 다루느냐가 가장 비싸다** — 아직 불러오는 중인데 "없어요"를
 * 띄우면 사용자는 챌린지가 사라진 줄 알고, 진짜 빈 상태에서 안내가 없으면 뭘 해야 할지 모른다.
 *
 * 기대 문구 출처: Figma `1134:2033`「홈 · 빈 상태」.
 */
@RunWith(RobolectricTestRunner::class)
class HomeContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `챌린지가 없으면 무엇을 할 수 있는지 두 갈래로 안내한다`() {
        // 둘러보기와 직접 만들기 둘 다 줘야 한다 — 하나만 있으면 다른 쪽을 원하는 사용자가 막힌다.
        render(HomeState(isLoading = false, challenges = emptyList(), filter = HomeFilter.ACTIVE))

        compose.onNodeWithText("첫 습관을 시작해 볼까요?").assertExists()
        compose.onNodeWithText("챌린지 둘러보기").assertExists()
        compose.onNodeWithText("직접 만들기").assertExists()
    }

    @Test
    fun `아직 불러오는 중이면 없어요를 띄우지 않는다`() {
        // 곧 채워질 화면에 "없어요"가 스쳐 지나가면 사용자는 사라진 줄 안다.
        render(HomeState(isLoading = true, challenges = emptyList(), filter = HomeFilter.ACTIVE))

        compose.onNodeWithText("첫 습관을 시작해 볼까요?").assertDoesNotExist()
    }

    @Test
    fun `챌린지가 있으면 빈 상태 안내를 띄우지 않는다`() {
        render(state(card("ch1", title = "아침 6시 기상")))

        compose.onNodeWithText("아침 6시 기상").assertExists()
        compose.onNodeWithText("첫 습관을 시작해 볼까요?").assertDoesNotExist()
    }

    @Test
    fun `둘러보기를 누르면 탐색 의도가 올라간다`() {
        val intents = mutableListOf<HomeIntent>()
        render(HomeState(isLoading = false, challenges = emptyList(), filter = HomeFilter.ACTIVE)) { intents += it }

        compose.onNodeWithText("챌린지 둘러보기").clickPastGuard()

        assertTrue(intents.contains(HomeIntent.OpenExplore))
    }

    @Test
    fun `직접 만들기를 누르면 생성 의도가 올라간다`() {
        val intents = mutableListOf<HomeIntent>()
        render(HomeState(isLoading = false, challenges = emptyList(), filter = HomeFilter.ACTIVE)) { intents += it }

        compose.onNodeWithText("직접 만들기").clickPastGuard()

        assertTrue(intents.contains(HomeIntent.CreateChallenge))
    }

    @Test
    fun `오늘 할 일 탭은 오늘이 대상인 것만 센다`() {
        // 개수가 실제와 다르면 사용자가 할 일을 놓친다.
        render(state(card("ch1", todayTarget = true), card("ch2", todayTarget = false)))

        compose.onNodeWithText("오늘 할 일 1").assertExists()
        compose.onNodeWithText("진행 중 2").assertExists()
    }

    private fun state(vararg cards: HomeChallengeUi) = HomeState(isLoading = false, challenges = cards.toList(), filter = HomeFilter.ACTIVE)

    private fun card(
        id: String,
        title: String = "챌린지 $id",
        todayTarget: Boolean = true,
    ) = HomeChallengeUi(
        challengeId = id,
        title = title,
        subtitle = "오늘 시작 · 솔로",
        progress = 0f,
        todayTarget = todayTarget,
        iconRes = android.R.drawable.ic_menu_help,
        accentColor = Color.Gray,
    )

    private fun render(
        state: HomeState,
        onIntent: (HomeIntent) -> Unit = {},
    ) {
        compose.setContent {
            RuleUpTheme {
                CompositionLocalProvider(LocalObservability provides testObservability()) {
                    HomeContent(state = state, onIntent = onIntent)
                }
            }
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.clickPastGuard() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(ClickClock.nextOffsetMillis()))
        performClick()
    }
}
