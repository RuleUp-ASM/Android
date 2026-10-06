package com.ruleup.home.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.ClickClock
import com.ruleup.domain.time.ServiceDate
import com.ruleup.home.presentation.viewmodel.DayResult
import com.ruleup.home.presentation.viewmodel.HomeIntent
import com.ruleup.home.presentation.viewmodel.HomeState
import com.ruleup.home.presentation.viewmodel.SelectedDay
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.profile.domain.entity.CalendarDayItem
import com.ruleup.profile.domain.entity.DayItemStatus
import com.ruleup.ui.helper.LocalObservability
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import kotlin.test.assertTrue

/** 홈. */
@RunWith(RobolectricTestRunner::class)
class HomeContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `챌린지가 없으면 무엇을 할 수 있는지 두 갈래로 안내한다`() {
        render(HomeState(isLoading = false, challenges = emptyList()))

        compose.onNodeWithText("첫 습관을 시작해 볼까요?").assertExists()
        compose.onNodeWithText("챌린지 둘러보기").assertExists()
        compose.onNodeWithText("직접 만들기").assertExists()
    }

    @Test
    fun `아직 불러오는 중이면 없어요를 띄우지 않는다`() {
        // 곧 채워질 화면에 "없어요"가 스쳐 지나가면 사용자는 사라진 줄 안다.
        render(HomeState(isLoading = true, challenges = emptyList()))

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
        render(HomeState(isLoading = false, challenges = emptyList())) { intents += it }

        compose.onNodeWithText("챌린지 둘러보기").clickPastGuard()

        assertTrue(intents.contains(HomeIntent.OpenExplore))
    }

    @Test
    fun `직접 만들기를 누르면 생성 의도가 올라간다`() {
        val intents = mutableListOf<HomeIntent>()
        render(HomeState(isLoading = false, challenges = emptyList())) { intents += it }

        compose.onNodeWithText("직접 만들기").clickPastGuard()

        assertTrue(intents.contains(HomeIntent.CreateChallenge))
    }

    @Test
    fun `오늘 대상이 아닌 챌린지도 홈에서 사라지지 않는다`() {
        // 오늘 대상이 아닌 챌린지도 홈에서 사라지면 안 된다(#570).
        render(state(card("ch1", todayTarget = true), card("ch2", todayTarget = false)))

        compose.onNodeWithText("챌린지 ch1").assertExists()
        compose.onNodeWithText("챌린지 ch2").assertExists()
    }

    @Test
    fun `매일 루틴과 주 N회 루틴을 다른 칸에 나눠 보여 준다`() {
        render(state(card("daily", weeklyCount = 7), card("weekly", weeklyCount = 3)))

        compose.onNodeWithText("오늘 · 매일 루틴").assertExists()
        compose.onNodeWithText("이번 주 · 주 N회").assertExists()
        compose.onNodeWithText("주 3회", substring = true).assertExists()
    }

    @Test
    fun `직접 체크할 챌린지가 있으면 오늘 해 볼 것으로 크게 띄운다`() {
        val intents = mutableListOf<HomeIntent>()
        render(state(card("ch1", title = "영단어 30개")).copy(manualCheckable = mapOf("ch1" to true))) { intents += it }

        compose.onNodeWithText("오늘 해 볼까요?").assertExists()
        compose.onNodeWithText("체크하기").clickPastGuard()

        assertTrue(intents.contains(HomeIntent.OpenChallenge("ch1")))
    }

    @Test
    fun `자동 인증만 있으면 오늘 해 볼 것을 띄우지 않는다`() {
        render(state(card("ch1")).copy(manualCheckable = mapOf("ch1" to false)))

        compose.onNodeWithText("오늘 해 볼까요?").assertDoesNotExist()
    }

    @Test
    fun `닉네임을 받았으면 인사에 붙인다`() {
        render(state(card("ch1")).copy(nickname = "지수"))

        compose.onNodeWithText("지수님", substring = true).assertExists()
    }

    @Test
    fun `첫 챌린지 후보가 있으면 빈 안내 대신 추천 카드를 보여 준다`() {
        val intents = mutableListOf<HomeIntent>()
        render(HomeState(isLoading = false, challenges = emptyList(), starters = listOf(starter("s1", "아침 러닝 30분")))) { intents += it }

        compose.onNodeWithText("이런 챌린지로 시작해 보세요").assertExists()
        compose.onNodeWithText("첫 습관을 시작해 볼까요?").assertDoesNotExist()
        compose.onNodeWithText("아침 러닝 30분").clickPastGuard()

        assertTrue(intents.contains(HomeIntent.OpenChallenge("s1")))
    }

    @Test
    fun `관심 분야 칩을 누르면 그 분야 둘러보기 의도가 올라간다`() {
        val intents = mutableListOf<HomeIntent>()
        render(
            HomeState(
                isLoading = false,
                challenges = emptyList(),
                starters = listOf(starter("s1", "아침 러닝 30분")),
                interests = listOf(Category.READING),
            ),
        ) { intents += it }

        compose.onNodeWithText(Category.READING.label).clickPastGuard()

        assertTrue(intents.contains(HomeIntent.OpenCategory(Category.READING)))
    }

    private fun starter(
        id: String,
        title: String,
    ) = TrendingChallenge(
        rank = 1,
        challengeId = id,
        title = title,
        imageUrl = null,
        category = Category.EXERCISE,
        participantCount = 128,
        recentJoins24h = 3,
        verificationType = VerificationType.AUTO,
        minTier = null,
        joinable = true,
        endDate = null,
    )

    @Test
    fun `고른 날의 결과가 오면 매일 루틴 대신 수행한 루틴과 실패한 루틴을 보여 준다`() {
        val done = dayItem("d1", "아침 6시 기상", DayItemStatus.DONE)
        val failed = dayItem("f1", "밤 11시 이후 폰 끄기", DayItemStatus.FAILED)
        val date = ServiceDate.today().minusDays(1).toString()
        render(state(card("ch1")).copy(selectedDay = SelectedDay(date, DayResult.Loaded(listOf(done, failed)))))

        compose.onNodeWithText("수행한 루틴 1").assertExists()
        compose.onNodeWithText("실패한 루틴 1").assertExists()
        compose.onNodeWithText("밤 11시 이후 폰 끄기").assertExists()
        compose.onNodeWithText("오늘 · 매일 루틴").assertDoesNotExist()
    }

    @Test
    fun `고른 날에 판정할 루틴이 없었으면 그렇게 알려 준다`() {
        val date = ServiceDate.today().minusDays(1).toString()
        render(state(card("ch1")).copy(selectedDay = SelectedDay(date, DayResult.Loaded(emptyList()))))

        compose.onNodeWithText("이날은 판정할 루틴이 없었어요").assertExists()
    }

    private fun dayItem(
        id: String,
        title: String,
        status: DayItemStatus,
    ) = CalendarDayItem(id, title, null, status, null, null, null, null, null)

    private fun state(vararg cards: HomeChallengeUi) = HomeState(isLoading = false, challenges = cards.toList())

    private fun card(
        id: String,
        title: String = "챌린지 $id",
        todayTarget: Boolean = true,
        weeklyCount: Int? = 7,
    ) = HomeChallengeUi(
        challengeId = id,
        title = title,
        subtitle = "진행중 · 솔로",
        progress = 0f,
        todayTarget = todayTarget,
        category = null,
        weeklyCount = weeklyCount,
        todayStatus = null,
        imageUrl = null,
        active = true,
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
