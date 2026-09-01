package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.observability.ChallengeCardSource
import com.ruleup.challenge.domain.observability.ChallengeEvents
import com.ruleup.challenge.domain.observability.ExploreListEntry
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.exploreChallenge
import com.ruleup.challenge.presentation.fake.exploreResult
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.test.RecordingSink
import com.ruleup.observability.domain.test.testObservability
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 둘러보기 목록의 **전이 전체** — 인텐트 → 조회 → 상태·이동·관측 — 를 본다.
 *
 * 개별 값 규칙(정렬 6종·필터 배지 수·빈 결과 사유·이벤트 스키마)은 아래층이 이미 끝냈다
 * ([com.ruleup.challenge.domain.entity.ExploreSortTest], `ExploreListStateTest`, `ChallengeEventsTest`).
 * 여기서 다시 훑지 않고, **그것들을 옳게 엮었는지**만 본다.
 *
 * 이 화면이 특히 위험한 지점은 두 가지다. ① 서버가 조건을 거절하면 사용자에게 되묻지 않고 스스로
 * 고쳐 재조회한다 — 고치는 규칙이 틀리면 사용자는 이유 없는 빈 화면을 본다. ② 노출·클릭 이벤트가
 * 탐색→참여 전환율의 분모·분자라, 중복이나 누락이 나면 릴리즈 게이트 지표가 조용히 틀어진다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreListViewModelTest {
    private val sink = RecordingSink()
    private val nav = RecordingNavigationHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: ExploreRepository = FakeExploreRepository()) =
        ExploreListViewModel(
            exploreRepository = repository,
            navigationHelper = nav,
            observability = testObservability(sink = sink),
        )

    private val eventNames: List<String>
        get() = sink.payloads.map { (it as BusinessPayload.Custom).name }

    // ---------- 진입 ----------

    @Test
    fun `첫 그림은 아무 조건도 걸리지 않은 로딩 화면이다`() {
        // 진입 직후 "결과 없음"을 그리면 매번 빈 화면이 한 프레임 스친다.
        val state = ExploreListState.initial

        assertTrue(state.isLoading)
        assertEquals(ExploreFilter.none, state.filter)
        assertEquals(ExploreSort.default, state.sort)
        assertTrue(state.items.isEmpty())
        assertNull(state.nextCursor)
    }

    @Test
    fun `카테고리 타일로 들어오면 그 카테고리를 필터에 걸고 카테고리 진입으로 기록한다`() =
        runTest {
            val repository = FakeExploreRepository()
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = "RECENT"))

            val prefilled = ExploreFilter(categories = setOf(Category.EXERCISE))
            assertEquals(
                listOf(FakeExploreRepository.Call(prefilled, ExploreSort.RECENT, null)),
                repository.calls,
            )
            assertEquals(prefilled, viewModel.uiState.value.filter)
            assertEquals(
                ChallengeEvents.exploreListView(ExploreListEntry.CATEGORY, ExploreSort.RECENT, prefilled),
                sink.payloads.first(),
            )
        }

    @Test
    fun `카테고리 없이 들어오면 전체 진입으로 기록한다`() =
        runTest {
            // 진입점을 나누지 못하면 카테고리 경로와 전체 경로의 전환율을 갈라 볼 수 없다.
            val viewModel = viewModel()

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(
                ChallengeEvents.exploreListView(ExploreListEntry.ALL, ExploreSort.POPULAR, ExploreFilter.none),
                sink.payloads.first(),
            )
        }

    @Test
    fun `모르는 카테고리·정렬 값으로 들어와도 전체 목록을 기본 정렬로 연다`() =
        runTest {
            // 서버가 폐기 code 를 링크에 남겨도 화면이 비지 않아야 한다.
            val repository = FakeExploreRepository()

            viewModel(repository).onIntent(ExploreListIntent.Load(category = "TIDYING", sort = "TEMPLATE_USAGE"))

            assertEquals(
                listOf(FakeExploreRepository.Call(ExploreFilter.none, ExploreSort.POPULAR, null)),
                repository.calls,
            )
        }

    @Test
    fun `이미 열어 본 화면으로 다시 돌아와도 재조회하지 않는다`() =
        runTest {
            // 화면 복귀마다 다시 조회하면 스크롤 위치가 사라지고 진입 이벤트가 부풀어 전환율 분모가 망가진다.
            val repository = FakeExploreRepository()
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.calls.size)
            assertEquals(listOf("explore_list_view", "explore_empty_result"), eventNames)
        }

    @Test
    fun `첫 페이지를 받으면 로딩을 끄고 다음 커서를 들고 있는다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(Result.success(exploreResult(listOf(exploreChallenge("c1")), nextCursor = "cur1"))),
                )
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(listOf("c1"), state.items.map { it.challengeId })
            assertEquals("cur1", state.nextCursor)
            assertNull(state.errorMessage)
        }

    @Test
    fun `첫 페이지가 0건이면 빈 결과를 기록한다`() =
        runTest {
            // 빈 결과율은 탐색 가드레일(10% 이하)의 분자다. 안 보내면 지표 자체가 만들어지지 않는다.
            val viewModel = viewModel(FakeExploreRepository(listOf(Result.success(exploreResult()))))

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(
                ChallengeEvents.exploreEmptyResult(ExploreFilter.none, ExploreSort.POPULAR),
                sink.payloads.last(),
            )
        }

    // ---------- 이어 붙이기 ----------

    @Test
    fun `커서가 남아 있으면 다음 페이지를 이어 붙이고 스크롤 깊이를 올려 기록한다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult(listOf(exploreChallenge("c1")), nextCursor = "cur1")),
                        Result.success(exploreResult(listOf(exploreChallenge("c2")), nextCursor = null)),
                    ),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals("cur1", repository.calls[1].cursor)
            assertEquals(listOf("c1", "c2"), viewModel.uiState.value.items.map { it.challengeId })
            assertNull(viewModel.uiState.value.nextCursor)
            assertEquals(ChallengeEvents.exploreListLoadMore(1, ExploreSort.POPULAR), sink.payloads.last())
        }

    @Test
    fun `마지막 페이지에 닿으면 더 요청하지 않는다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(Result.success(exploreResult(listOf(exploreChallenge("c1")), nextCursor = null))),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals(1, repository.calls.size)
        }

    @Test
    fun `이미 다음 페이지를 불러오는 중이면 겹쳐 요청하지 않는다`() =
        runTest {
            // 스크롤은 하단 근접마다 인텐트를 올린다. 잠그지 않으면 같은 커서를 여러 번 보내 같은 카드가 겹친다.
            val repository =
                FakeExploreRepository(
                    listOf(Result.success(exploreResult(listOf(exploreChallenge("c1")), nextCursor = "cur1"))),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            val gate = CompletableDeferred<Unit>()
            repository.gate = gate

            viewModel.onIntent(ExploreListIntent.LoadMore)
            viewModel.onIntent(ExploreListIntent.LoadMore)
            gate.complete(Unit)

            assertEquals(2, repository.calls.size)
        }

    @Test
    fun `다음 페이지가 실패해도 이미 받은 목록은 지우지 않는다`() =
        runTest {
            // 목록을 비우면 사용자는 스크롤하다 갑자기 빈 화면을 본다. 하단에서만 다시 시도하게 한다.
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult(listOf(exploreChallenge("c1")), nextCursor = "cur1")),
                        Result.failure(RuntimeException("network")),
                    ),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            val state = viewModel.uiState.value
            assertEquals(listOf("c1"), state.items.map { it.challengeId })
            assertTrue(state.loadMoreFailed)
            assertFalse(state.isLoadingMore)
            assertNull(state.errorMessage)
        }

    @Test
    fun `다음 페이지에서 커서가 상하면 알리지 않고 첫 페이지부터 다시 받는다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult(listOf(exploreChallenge("c1")), nextCursor = "cur1")),
                        Result.failure(CursorInvalidException()),
                        Result.success(exploreResult(listOf(exploreChallenge("c9")), nextCursor = null)),
                    ),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertNull(repository.calls[2].cursor)
            val state = viewModel.uiState.value
            assertEquals(listOf("c9"), state.items.map { it.challengeId })
            assertFalse(state.loadMoreFailed)
            assertNull(state.errorMessage)
        }

    // ---------- 조건 변경 ----------

    @Test
    fun `필터를 바꾸면 커서를 버리고 첫 페이지부터 다시 조회한다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult(listOf(exploreChallenge("c1")), nextCursor = "cur1")),
                        Result.success(exploreResult(listOf(exploreChallenge("c2")), nextCursor = null)),
                    ),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            val filter = ExploreFilter(categories = setOf(Category.EXERCISE), verifyType = VerificationType.AUTO)

            viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))

            assertEquals(FakeExploreRepository.Call(filter, ExploreSort.POPULAR, null), repository.calls[1])
            assertEquals(listOf("c2"), viewModel.uiState.value.items.map { it.challengeId })
            assertEquals(filter, viewModel.uiState.value.filter)
        }

    @Test
    fun `필터 적용은 결과가 몇 건인지까지 함께 기록한다`() =
        runTest {
            // result_count 는 응답이 와야 알 수 있다. 인텐트 시점에 보내면 필터 사용률과 빈 결과율이 어긋난다.
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult(listOf(exploreChallenge("c1")))),
                        Result.success(exploreResult(listOf(exploreChallenge("c2"), exploreChallenge("c3")))),
                    ),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            val filter = ExploreFilter(eligibleOnly = true)

            viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))

            assertEquals(ChallengeEvents.exploreFilterApply(filter, 2), sink.payloads.last())
        }

    @Test
    fun `정렬을 바꾸면 어디서 어디로 옮겼는지와 함께 기록하고 첫 페이지부터 다시 조회한다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult(listOf(exploreChallenge("c1")), nextCursor = "cur1")),
                        Result.success(exploreResult(listOf(exploreChallenge("c2")))),
                    ),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.DEADLINE))

            assertEquals(
                FakeExploreRepository.Call(ExploreFilter.none, ExploreSort.DEADLINE, null),
                repository.calls[1],
            )
            assertEquals(ExploreSort.DEADLINE, viewModel.uiState.value.sort)
            assertEquals(
                ChallengeEvents.exploreSortChange(ExploreSort.POPULAR, ExploreSort.DEADLINE, 1),
                sink.payloads.last(),
            )
        }

    @Test
    fun `티어 조건만 끄고 나머지 필터는 그대로 둔 채 다시 조회한다`() =
        runTest {
            // 빈 결과에서 제안하는 완화다. 여기서 카테고리까지 날리면 사용자가 고른 조건이 말없이 사라진다.
            val repository = FakeExploreRepository()
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))
            viewModel.onIntent(
                ExploreListIntent.ApplyFilter(
                    ExploreFilter(categories = setOf(Category.EXERCISE), eligibleOnly = true),
                ),
            )

            viewModel.onIntent(ExploreListIntent.ClearEligibleOnly)

            assertEquals(
                ExploreFilter(categories = setOf(Category.EXERCISE), eligibleOnly = false),
                repository.calls.last().filter,
            )
        }

    // ---------- 서버가 조건을 거절할 때 ----------

    @Test
    fun `서버가 정렬을 거절하면 되묻지 않고 기본 정렬로 다시 조회한다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.failure(InvalidSortTypeException()),
                        Result.success(exploreResult(listOf(exploreChallenge("c1")))),
                    ),
                )
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = "DEADLINE"))

            assertEquals(listOf(ExploreSort.DEADLINE, ExploreSort.POPULAR), repository.calls.map { it.sort })
            assertEquals(ExploreSort.POPULAR, viewModel.uiState.value.sort)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `기본 정렬로도 거절당하면 더 고칠 게 없으니 오류를 보여준다`() =
        runTest {
            // 여기서 또 기본 정렬로 재시도하면 같은 요청을 무한히 되풀이한다.
            val repository = FakeExploreRepository(listOf(Result.failure(InvalidSortTypeException())))
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.calls.size)
            assertNotNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `서버가 필터를 거절하면 필터를 비우고 다시 조회한다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.failure(InvalidFilterValueException()),
                        Result.success(exploreResult(listOf(exploreChallenge("c1")))),
                    ),
                )
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))

            assertEquals(ExploreFilter.none, repository.calls[1].filter)
            assertEquals(ExploreFilter.none, viewModel.uiState.value.filter)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `필터가 이미 비었는데 거절당하면 오류를 보여준다`() =
        runTest {
            val repository = FakeExploreRepository(listOf(Result.failure(InvalidFilterValueException())))
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.calls.size)
            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `실패 원인에 할 말이 없어도 빈 화면 대신 안내 문구를 보여준다`() =
        runTest {
            // message 가 null 인 예외(취소·IO)를 그대로 흘리면 화면이 아무 설명 없이 비어 버린다.
            val repository = FakeExploreRepository(listOf(Result.failure(RuntimeException())))
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals("챌린지를 불러오지 못했어요", viewModel.uiState.value.errorMessage)
        }

    // ---------- 노출·클릭 ----------

    @Test
    fun `같은 카드가 스크롤로 다시 보여도 노출은 한 번만 기록한다`() =
        runTest {
            // 노출이 부풀면 상세 진입률(클릭/노출)이 실제보다 낮게 나와 탐색 품질을 잘못 판단한다.
            val item = exploreChallenge("c1", isFull = true, eligible = false)
            val repository = FakeExploreRepository(listOf(Result.success(exploreResult(listOf(item)))))
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.CardImpression("c1"))
            viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

            assertEquals(
                listOf(
                    ChallengeEvents.challengeCardImpression(
                        challengeId = "c1",
                        position = 0,
                        sort = ExploreSort.POPULAR,
                        isFull = true,
                        eligible = false,
                        hasMetrics = true,
                    ),
                ),
                sink.payloads
                    .filterIsInstance<BusinessPayload.Custom>()
                    .filter { it.name == "challenge_card_impression" },
            )
        }

    @Test
    fun `조건이 바뀌어 목록이 새로 오면 노출을 다시 기록한다`() =
        runTest {
            // 필터를 바꾸면 다른 목록이다. 이전 세션의 중복 제거를 끌고 가면 새 목록의 노출이 통째로 사라진다.
            val item = exploreChallenge("c1")
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult(listOf(item))),
                        Result.success(exploreResult(listOf(item))),
                    ),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

            viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.RECENT))
            viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

            assertEquals(2, eventNames.count { it == "challenge_card_impression" })
        }

    @Test
    fun `목록에 없는 카드는 노출로 기록하지 않는다`() =
        runTest {
            // position 을 지어내면 순위별 노출 분포가 오염된다.
            val repository =
                FakeExploreRepository(listOf(Result.success(exploreResult(listOf(exploreChallenge("c1"))))))
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.CardImpression("사라진-카드"))

            assertEquals(0, eventNames.count { it == "challenge_card_impression" })
        }

    @Test
    fun `카드를 누르면 몇 번째였는지와 함께 기록하고 상세로 넘긴다`() =
        runTest {
            // 같은 challenge_id 가 노출→클릭→상세로 이어져야 전환율이 계산된다.
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(
                            exploreResult(listOf(exploreChallenge("c1"), exploreChallenge("c2"))),
                        ),
                    ),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.OpenChallenge("c2"))

            assertEquals(
                ChallengeEvents.challengeCardClick("c2", 1, ChallengeCardSource.LIST, ExploreSort.POPULAR),
                sink.payloads.last(),
            )
            assertEquals(listOf(ChallengeDetailPage("c2").toRoute()), nav.routes)
        }

    @Test
    fun `뒤로가기는 조회도 기록도 없이 이전 화면으로 되돌린다`() =
        runTest {
            val repository = FakeExploreRepository()
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Back)

            assertEquals(1, nav.backCount)
            assertTrue(repository.calls.isEmpty())
            assertTrue(sink.payloads.isEmpty())
        }
}

/**
 * 인텐트마다 그것을 지키는 테스트 이름. **인텐트가 늘면 이 `when` 이 컴파일되지 않아** 미검증이
 * 드러난다 — 아무도 호출하지 않아도 되고, 존재하는 것 자체가 열거를 끝냈다는 근거다.
 */
private fun ExploreListIntent.coveredBy(): String =
    when (this) {
        is ExploreListIntent.Load ->
            "카테고리 타일로 들어오면 그 카테고리를 필터에 걸고 카테고리 진입으로 기록한다 / " +
                "카테고리 없이 들어오면 전체 진입으로 기록한다 / " +
                "모르는 카테고리·정렬 값으로 들어와도 전체 목록을 기본 정렬로 연다 / " +
                "이미 열어 본 화면으로 다시 돌아와도 재조회하지 않는다"
        ExploreListIntent.LoadMore ->
            "커서가 남아 있으면 다음 페이지를 이어 붙이고 스크롤 깊이를 올려 기록한다 / " +
                "마지막 페이지에 닿으면 더 요청하지 않는다 / " +
                "이미 다음 페이지를 불러오는 중이면 겹쳐 요청하지 않는다 / " +
                "다음 페이지가 실패해도 이미 받은 목록은 지우지 않는다 / " +
                "다음 페이지에서 커서가 상하면 알리지 않고 첫 페이지부터 다시 받는다"
        is ExploreListIntent.ApplyFilter ->
            "필터를 바꾸면 커서를 버리고 첫 페이지부터 다시 조회한다 / " +
                "필터 적용은 결과가 몇 건인지까지 함께 기록한다"
        is ExploreListIntent.SelectSort ->
            "정렬을 바꾸면 어디서 어디로 옮겼는지와 함께 기록하고 첫 페이지부터 다시 조회한다"
        ExploreListIntent.ClearEligibleOnly ->
            "티어 조건만 끄고 나머지 필터는 그대로 둔 채 다시 조회한다"
        is ExploreListIntent.CardImpression ->
            "같은 카드가 스크롤로 다시 보여도 노출은 한 번만 기록한다 / " +
                "조건이 바뀌어 목록이 새로 오면 노출을 다시 기록한다 / " +
                "목록에 없는 카드는 노출로 기록하지 않는다"
        is ExploreListIntent.OpenChallenge ->
            "카드를 누르면 몇 번째였는지와 함께 기록하고 상세로 넘긴다"
        ExploreListIntent.Back ->
            "뒤로가기는 조회도 기록도 없이 이전 화면으로 되돌린다"
    }
