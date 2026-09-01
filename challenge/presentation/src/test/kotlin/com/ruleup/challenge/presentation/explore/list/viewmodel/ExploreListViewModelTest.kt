package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.api.Observability
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 둘러보기 목록. 이 화면은 서버가 조건을 거절하면 **사용자에게 묻지 않고 스스로 조건을 되돌려**
 * 다시 조회한다 — 되돌릴 곳이 남았는지가 재시도와 실패를 가른다. 그래서 "몇 번, 어떤 조건으로
 * 나갔는가"가 상태만큼이나 계약이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreListViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `처음 열면 첫 페이지를 한 번만 조회한다`() =
        runTest {
            val repo = repo(page("ch1", "ch2"))
            val viewModel = viewModel(repo)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repo.exploreQueries.size)
            assertEquals(
                listOf("ch1", "ch2"),
                viewModel.uiState.value.items
                    .map { it.challengeId },
            )
        }

    @Test
    fun `서버가 정렬을 거절하면 기본 정렬로 되돌려 다시 묻는다`() =
        runTest {
            // 사용자에게 되묻지 않고 스스로 고친다 — 화면이 빈 채로 멈추는 게 더 나쁘다.
            var first = true
            val repo =
                FakeExploreRepository(
                    explore = { _, _, _ ->
                        if (first) {
                            first = false
                            throw InvalidSortTypeException()
                        }
                        page("ch1")
                    },
                )
            val viewModel = viewModel(repo)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = ExploreSort.DEADLINE.value))

            assertEquals(ExploreSort.default, repo.exploreQueries.last().second)
            assertEquals(
                listOf("ch1"),
                viewModel.uiState.value.items
                    .map { it.challengeId },
            )
        }

    @Test
    fun `기본 정렬인데도 거절당하면 더 되돌릴 곳이 없어 실패로 남긴다`() =
        runTest {
            val repo = FakeExploreRepository(explore = { _, _, _ -> throw InvalidSortTypeException() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repo.exploreQueries.size)
            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `서버가 필터를 거절하면 필터를 풀고 다시 묻는다`() =
        runTest {
            var first = true
            val repo =
                FakeExploreRepository(
                    explore = { _, _, _ ->
                        if (first) {
                            first = false
                            throw InvalidFilterValueException()
                        }
                        page("ch1")
                    },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(ExploreListIntent.Load(category = Category.entries.first().value, sort = null))

            assertEquals(ExploreFilter.none, repo.exploreQueries.last().first)
        }

    @Test
    fun `필터가 이미 비었는데 거절당하면 실패로 남긴다`() =
        runTest {
            val repo = FakeExploreRepository(explore = { _, _, _ -> throw InvalidFilterValueException() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repo.exploreQueries.size)
            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `다음 페이지는 커서를 실어 이어붙인다`() =
        runTest {
            val repo =
                FakeExploreRepository(
                    explore = { _, _, cursor ->
                        if (cursor == null) page("ch1", nextCursor = "c1") else page("ch2")
                    },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals("c1", repo.exploreQueries.last().third)
            assertEquals(
                listOf("ch1", "ch2"),
                viewModel.uiState.value.items
                    .map { it.challengeId },
            )
        }

    @Test
    fun `마지막 페이지에서는 더 묻지 않는다`() =
        runTest {
            val repo = repo(page("ch1", nextCursor = null))
            val viewModel = viewModel(repo)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals(1, repo.exploreQueries.size)
        }

    @Test
    fun `다음 페이지 조회에 실패해도 이미 받은 목록은 남긴다`() =
        runTest {
            // 여기서 목록을 비우면 사용자가 보던 화면이 통째로 사라진다.
            val repo =
                FakeExploreRepository(
                    explore = { _, _, cursor ->
                        if (cursor == null) page("ch1", nextCursor = "c1") else throw IllegalStateException("네트워크 오류")
                    },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals(
                listOf("ch1"),
                viewModel.uiState.value.items
                    .map { it.challengeId },
            )
        }

    @Test
    fun `정렬을 바꾸면 첫 페이지부터 다시 조회한다`() =
        runTest {
            val repo = repo(page("ch1", nextCursor = "c1"))
            val viewModel = viewModel(repo)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.RECENT))

            assertNull(repo.exploreQueries.last().third)
            assertEquals(ExploreSort.RECENT, repo.exploreQueries.last().second)
        }

    @Test
    fun `조건에 맞는 게 없으면 목록을 비우고 빈 결과로 둔다`() =
        runTest {
            val viewModel = viewModel(repo(page()))

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertTrue(
                viewModel.uiState.value.items
                    .isEmpty(),
            )
            assertNull(viewModel.uiState.value.errorMessage)
        }

    private fun repo(result: ExploreResult) = FakeExploreRepository(explore = { _, _, _ -> result })

    private fun page(
        vararg ids: String,
        nextCursor: String? = null,
    ) = ExploreResult(
        items = ids.map { challenge(it) },
        nextCursor = nextCursor,
        hasNext = nextCursor != null,
    )

    private fun challenge(id: String) =
        ExploreChallenge(
            challengeId = id,
            title = "챌린지 $id",
            imageUrl = null,
            category = Category.entries.first(),
            verificationType = VerificationType.entries.first(),
            startsSoon = false,
            participantCount = 3,
            capacity = 10,
            isFull = false,
            minTier = null,
            eligible = true,
            completionRate = null,
            retentionRate = null,
            dday = null,
            startDate = null,
            endDate = null,
            createdAt = null,
        )

    private fun viewModel(
        repo: FakeExploreRepository = FakeExploreRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        observability: Observability = testObservability(),
    ) = ExploreListViewModel(exploreRepository = repo, navigationHelper = nav, observability = observability)
}
