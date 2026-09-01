package com.ruleup.profile.presentation.home.viewmodel

import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyHomeCounts
import com.ruleup.profile.presentation.fake.FakeMyPageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 마이 홈. 화면 복귀(ON_RESUME)마다 조용히 갱신하는 화면이라, **이미 보여 주고 있는 내용을
 * 망가뜨리지 않는 것**이 핵심이다 — 갱신에 실패했다고 멀쩡히 보이던 프로필을 오류 화면으로
 * 바꾸면 사용자는 없던 사고를 본다.
 *
 * 랭킹 진입은 참여 중인 그룹 수에 따라 갈린다(0 = 안내 · 1 = 바로 · 2+ = 선택).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyHomeViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 프로필을 화면에 올린다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(home = { home(nickname = "지현") }))

            viewModel.onIntent(MyHomeIntent.Load)

            assertEquals(
                "지현",
                viewModel.uiState.value.home
                    ?.nickname,
            )
        }

    @Test
    fun `이미 받아둔 프로필이 있으면 다시 묻지 않는다`() =
        runTest {
            val repo = FakeMyPageRepository(home = { home() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyHomeIntent.Load)
            viewModel.onIntent(MyHomeIntent.Load)

            assertEquals(1, repo.calls.count { it == "getHome" })
        }

    @Test
    fun `화면으로 돌아오면 받아둔 게 있어도 새로 갱신한다`() =
        runTest {
            val repo = FakeMyPageRepository(home = { home() })
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyHomeIntent.Load)

            viewModel.onIntent(MyHomeIntent.Refresh)

            assertEquals(2, repo.calls.count { it == "getHome" })
        }

    @Test
    fun `갱신에 실패해도 보여 주던 프로필을 오류 화면으로 바꾸지 않는다`() =
        runTest {
            // 복귀마다 도는 조용한 갱신이다. 여기서 오류를 띄우면 없던 사고를 보여 주는 셈이다.
            var fail = false
            val viewModel =
                viewModel(
                    FakeMyPageRepository(home = { if (fail) throw IllegalStateException("네트워크 끊김") else home(nickname = "지현") }),
                )
            viewModel.onIntent(MyHomeIntent.Load)

            fail = true
            viewModel.onIntent(MyHomeIntent.Refresh)

            assertEquals(
                "지현",
                viewModel.uiState.value.home
                    ?.nickname,
            )
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `첫 조회부터 실패하면 보여 줄 게 없으니 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(home = { throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(MyHomeIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `참여 중인 그룹이 없으면 랭킹으로 보내지 않고 이유를 알린다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(FakeMyPageRepository(groupChallenges = { emptyList() }), nav)
            val effects = collectEffects(viewModel)

            viewModel.onIntent(MyHomeIntent.OpenRanking)

            assertEquals(listOf(MyHomeEffect.ShowMessage("참여 중인 그룹 챌린지가 없어요")), effects)
            assertTrue(nav.didNotMove)
        }

    @Test
    fun `참여 중인 그룹이 하나면 고르게 하지 않고 바로 그 랭킹으로 간다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(FakeMyPageRepository(groupChallenges = { listOf(group("ch1")) }), nav)

            viewModel.onIntent(MyHomeIntent.OpenRanking)

            assertEquals(mapOf("challengeId" to "ch1"), nav.routes.single().args)
            assertNull(viewModel.uiState.value.rankingPicker)
        }

    @Test
    fun `참여 중인 그룹이 여럿이면 고르게 하고 아직 이동하지 않는다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel =
                viewModel(FakeMyPageRepository(groupChallenges = { listOf(group("ch1"), group("ch2")) }), nav)

            viewModel.onIntent(MyHomeIntent.OpenRanking)

            assertEquals(
                2,
                viewModel.uiState.value.rankingPicker
                    ?.size,
            )
            assertTrue(nav.didNotMove)
        }

    @Test
    fun `고른 챌린지의 랭킹으로 가면서 선택 시트를 닫는다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel =
                viewModel(FakeMyPageRepository(groupChallenges = { listOf(group("ch1"), group("ch2")) }), nav)
            viewModel.onIntent(MyHomeIntent.OpenRanking)

            viewModel.onIntent(MyHomeIntent.SelectRankingChallenge("ch2"))

            assertEquals(mapOf("challengeId" to "ch2"), nav.routes.single().args)
            assertNull(viewModel.uiState.value.rankingPicker)
        }

    @Test
    fun `그룹 조회에 실패하면 이동하지 않고 이유를 알린다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(FakeMyPageRepository(groupChallenges = { throw IllegalStateException("그룹 조회 실패") }), nav)
            val effects = collectEffects(viewModel)

            viewModel.onIntent(MyHomeIntent.OpenRanking)

            assertEquals(listOf(MyHomeEffect.ShowMessage("그룹 조회 실패")), effects)
            assertTrue(nav.didNotMove)
        }

    private fun TestScope.collectEffects(viewModel: MyHomeViewModel): List<MyHomeEffect> {
        val effects = mutableListOf<MyHomeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyHomeViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun home(nickname: String = "지현") =
        MyHome(
            nickname = nickname,
            nicknameStatus = NicknameStatus.APPROVED,
            profileImageUrl = null,
            mannerTemperature = 36.5,
            counts = MyHomeCounts(completed = 2, inProgress = 1, groups = 1),
        )

    private fun group(id: String) = GroupChallengeSummary(challengeId = id, title = "아침 러닝")
}
