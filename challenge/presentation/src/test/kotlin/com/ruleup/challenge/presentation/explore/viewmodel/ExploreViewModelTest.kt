package com.ruleup.challenge.presentation.explore.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.test.RecordingSink
import com.ruleup.observability.domain.test.testObservability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 탐색 홈. 인기·카테고리 두 섹션이 **따로 실패할 수 있고**, 한쪽이 죽어도 나머지는 보여야 한다.
 *
 * 관측 쪽 계약이 더 까다롭다 — 탐색 홈 진입은 전환율의 **분모**라 두 섹션이 각각 끝나도 딱
 * 한 번만 나가야 한다. 두 번 나가면 분모가 부풀어 전환율이 실제보다 낮게 보인다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `인기는 상위 다섯 개까지만 홈에 올린다`() =
        runTest {
            val viewModel = viewModel(repo(trendingCount = 9))

            viewModel.onIntent(ExploreIntent.Load)

            assertEquals(5, viewModel.uiState.value.trending.size)
        }

    @Test
    fun `한 번 실패하면 곧바로 한 번 더 시도한다`() =
        runTest {
            var attempt = 0
            val repo =
                FakeExploreRepository(
                    trending = {
                        attempt++
                        if (attempt == 1) throw IllegalStateException("일시 오류") else snapshot(1)
                    },
                    categories = { emptyList() },
                )

            viewModel(repo).onIntent(ExploreIntent.Load)

            assertEquals(2, attempt)
        }

    @Test
    fun `인기가 죽어도 카테고리는 보여 준다`() =
        runTest {
            // 두 섹션은 서로 독립이다. 하나가 실패했다고 화면 전체를 비우면 안 된다.
            val viewModel =
                viewModel(
                    FakeExploreRepository(
                        trending = { throw IllegalStateException("인기 오류") },
                        categories = { listOf(categoryCount(Category.entries.first(), 3)) },
                    ),
                )

            viewModel.onIntent(ExploreIntent.Load)

            assertEquals(1, viewModel.uiState.value.categories.size)
        }

    @Test
    fun `탐색 홈 진입은 두 섹션이 다 끝나도 한 번만 센다`() =
        runTest {
            // 전환율의 분모다. 두 번 나가면 전환율이 실제보다 낮게 보인다.
            val sink = RecordingSink()
            val viewModel = viewModel(repo(trendingCount = 3), observability = testObservability(sink = sink))

            viewModel.onIntent(ExploreIntent.Load)

            assertEquals(1, sink.customNames.count { it == "explore_home_view" })
        }

    @Test
    fun `다시 시도해도 홈 진입을 또 세지 않는다`() =
        runTest {
            val sink = RecordingSink()
            val viewModel = viewModel(repo(trendingCount = 3), observability = testObservability(sink = sink))
            viewModel.onIntent(ExploreIntent.Load)

            viewModel.onIntent(ExploreIntent.RetryTrending)

            assertEquals(1, sink.customNames.count { it == "explore_home_view" })
        }

    @Test
    fun `인기가 비었으면 노출을 세지 않는다`() =
        runTest {
            // 보여 준 게 없는데 노출로 세면 노출 대비 클릭률이 0 으로 눌린다.
            val sink = RecordingSink()
            val viewModel = viewModel(repo(trendingCount = 0), observability = testObservability(sink = sink))

            viewModel.onIntent(ExploreIntent.Load)

            assertTrue(sink.customNames.none { it == "trending_impression" })
        }

    @Test
    fun `인기 카드를 누르면 그 챌린지 상세로 간다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repo(trendingCount = 3), nav)
            viewModel.onIntent(ExploreIntent.Load)

            viewModel.onIntent(ExploreIntent.OpenChallenge("ch2"))

            assertEquals(1, nav.routes.size)
            assertEquals("ch2", nav.routes.single().args["challengeId"])
        }

    @Test
    fun `카테고리를 누르면 그 카테고리로 거른 목록으로 간다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val category = Category.entries.first()
            val viewModel =
                viewModel(
                    FakeExploreRepository(trending = { snapshot(0) }, categories = { listOf(categoryCount(category, 3)) }),
                    nav,
                )
            viewModel.onIntent(ExploreIntent.Load)

            viewModel.onIntent(ExploreIntent.OpenCategory(category))

            assertEquals(category.value, nav.routes.single().args["category"])
        }

    @Test
    fun `이미 불러오는 중이면 다시 요청하지 않는다`() =
        runTest {
            val repo = repo(trendingCount = 3)
            val viewModel = viewModel(repo)

            viewModel.onIntent(ExploreIntent.Load)
            viewModel.onIntent(ExploreIntent.Load)

            // 1회 자동 재시도가 없는 성공 경로라 진입 1회당 조회 1회다.
            assertEquals(2, repo.calls.count { it == "getTrending" })
        }

    /** 이 카탈로그의 이벤트는 전부 [BusinessPayload.Custom] 이라 이름으로 센다. */
    private val RecordingSink.customNames: List<String>
        get() = payloads.filterIsInstance<BusinessPayload.Custom>().map { it.name }

    private fun repo(trendingCount: Int) = FakeExploreRepository(trending = { snapshot(trendingCount) }, categories = { emptyList() })

    private fun viewModel(
        repo: FakeExploreRepository = FakeExploreRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        observability: Observability = testObservability(),
    ) = ExploreViewModel(exploreRepository = repo, navigationHelper = nav, observability = observability)

    private fun snapshot(count: Int) =
        TrendingSnapshot(
            calculatedAt = "2026-09-01T00:00:00Z",
            items =
                (1..count).map {
                    TrendingChallenge(
                        rank = it,
                        challengeId = "ch$it",
                        title = "챌린지 $it",
                        imageUrl = null,
                        category = Category.entries.first(),
                        participantCount = 4,
                        recentJoins24h = 2,
                        verificationType = VerificationType.entries.first(),
                        minTier = null,
                        joinable = true,
                        endDate = null,
                    )
                },
        )

    private fun categoryCount(
        category: Category,
        count: Int,
    ) = ChallengeCategoryCount(name = category.value, activeGroupCount = count, category = category)
}
