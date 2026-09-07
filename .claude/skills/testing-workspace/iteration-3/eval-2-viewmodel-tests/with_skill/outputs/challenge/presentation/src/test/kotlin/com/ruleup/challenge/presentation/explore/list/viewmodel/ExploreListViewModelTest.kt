package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.exploreResult
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.model.AttrKey
import com.ruleup.observability.domain.model.AttrValue
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 둘러보기 목록의 인텐트 → 조회 → 상태 전이를 본다.
 *
 * 정렬 6종·필터 배지 같은 값 규칙은 `ExploreTest`(케이스 층)가, 빈 결과 문구 선택은
 * `ExploreListStateTest`(케이스 층)가 이미 끝냈으므로 여기서 다시 훑지 않는다. 여기 남는 건
 * **협력자를 옳게 엮었는가** — 언제 서버를 부르고, 안 부르고, 서버 거절을 스스로 고쳐 다시 부르고,
 * 어디로 이동하고, 어떤 이벤트를 몇 번 보내는가다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreListViewModelTest {
    @Before
    fun setUp() {
        // viewModelScope 는 Dispatchers.Main 을 쓴다. JVM 테스트엔 Main 이 없어 갈아끼우지 않으면 초기화부터 터진다.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------- 진입 ----------

    @Test
    fun `카테고리 타일로 들어오면 그 카테고리를 걸고 지정된 정렬로 첫 페이지를 조회한다`() =
        runTest {
            val repository = FakeExploreRepository()
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = "RECENT"))

            val query = repository.queries.single()
            assertEquals(setOf(Category.EXERCISE), query.filter.categories)
            assertEquals(ExploreSort.RECENT, query.sort)
            assertNull(query.cursor)
        }

    @Test
    fun `모르는 카테고리·정렬로 들어오면 전체 목록을 기본 정렬로 조회한다`() =
        runTest {
            // 서버가 폐기한 정렬 값이 딥링크에 남아 있어도 400 을 받으러 가지 않고 기본값으로 시작한다.
            val repository = FakeExploreRepository()
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.Load(category = "UNKNOWN", sort = "TEMPLATE_USAGE"))

            val query = repository.queries.single()
            assertEquals(ExploreFilter.none, query.filter)
            assertEquals(ExploreSort.default, query.sort)
        }

    @Test
    fun `이미 목록을 불러왔으면 다시 진입해도 조회하지도 진입을 다시 세지도 않는다`() =
        runTest {
            // 화면 재구성마다 Load 가 올라오는데 그때마다 부르면 목록이 튀고 진입 분모가 부풀어 전환율이 낮게 나온다.
            val repository = FakeExploreRepository()
            val sink = RecordingSink()
            val viewModel = viewModel(repository = repository, observability = testObservability(sink = sink))

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.queries.size)
            assertEquals(1, sink.eventNames.count { it == "explore_list_view" })
        }

    @Test
    fun `카테고리로 들어온 진입과 전체 진입을 진입 이벤트가 구분한다`() =
        runTest {
            val fromCategory = RecordingSink()
            val fromAll = RecordingSink()

            viewModel(observability = testObservability(sink = fromCategory))
                .onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))
            viewModel(observability = testObservability(sink = fromAll))
                .onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(AttrValue.Str("category"), fromCategory.attrsOf("explore_list_view", "entry").single())
            assertEquals(AttrValue.Str("all"), fromAll.attrsOf("explore_list_view", "entry").single())
        }

    // ---------- 첫 페이지 결과 ----------

    @Test
    fun `첫 페이지가 오면 로딩을 끝내고 다음 커서를 들고 있는다`() =
        runTest {
            val repository = FakeExploreRepository(listOf(Result.success(exploreResult("a", "b", nextCursor = "cursor-1"))))
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(listOf("a", "b"), state.items.map { it.challengeId })
            assertEquals("cursor-1", state.nextCursor)
            assertNull(state.errorMessage)
        }

    @Test
    fun `첫 페이지 조회가 실패하면 로딩을 끝내고 오류 문구를 남긴다`() =
        runTest {
            val repository = FakeExploreRepository(listOf(Result.failure(RuntimeException("네트워크가 불안정해요"))))
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("네트워크가 불안정해요", state.errorMessage)
            assertTrue(state.items.isEmpty())
        }

    // ---------- 서버 거절을 스스로 고치는 경로 ----------

    @Test
    fun `정렬을 서버가 거절하면 기본 정렬로 되돌려 다시 조회한다`() =
        runTest {
            // 사용자에게 되묻지 않는다 — 목록이 비어 보이는 시간을 만들지 않는 게 이 복구의 목적이다.
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.failure(InvalidSortTypeException()),
                        Result.success(exploreResult("a")),
                    ),
                )
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = "DEADLINE"))

            assertEquals(listOf(ExploreSort.DEADLINE, ExploreSort.default), repository.queries.map { it.sort })
            assertEquals(ExploreSort.default, viewModel.uiState.value.sort)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `기본 정렬로도 거절당하면 더 되돌리지 않고 오류를 알린다`() =
        runTest {
            // 되돌릴 곳이 없는데 계속 재시도하면 무한 호출이 된다.
            val repository = FakeExploreRepository(listOf(Result.failure(InvalidSortTypeException())))
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.queries.size)
            assertEquals("정렬 조건을 다시 선택해 주세요.", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `필터를 서버가 거절하면 필터를 비우고 다시 조회한다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.failure(InvalidFilterValueException()),
                        Result.success(exploreResult("a")),
                    ),
                )
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.READING))))

            assertEquals(ExploreFilter.none, repository.queries.last().filter)
            assertEquals(ExploreFilter.none, viewModel.uiState.value.filter)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `필터가 이미 비어 있는데 거절당하면 더 비우지 않고 오류를 알린다`() =
        runTest {
            val repository = FakeExploreRepository(listOf(Result.failure(InvalidFilterValueException())))
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.queries.size)
            assertEquals("필터 조건을 다시 선택해 주세요.", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `첫 페이지에서 커서가 상하면 사용자에게 알리지 않고 같은 조건으로 다시 조회한다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.failure(CursorInvalidException()),
                        Result.success(exploreResult("a")),
                    ),
                )
            val viewModel = viewModel(repository = repository)

            viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))

            assertEquals(2, repository.queries.size)
            assertEquals(setOf(Category.EXERCISE), repository.queries.last().filter.categories)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    // ---------- 이어붙이기 ----------

    @Test
    fun `커서가 남아 있으면 다음 페이지를 목록 뒤에 이어 붙인다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult("a", "b", nextCursor = "cursor-1")),
                        Result.success(exploreResult("c", nextCursor = null)),
                    ),
                )
            val viewModel = viewModel(repository = repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals("cursor-1", repository.queries.last().cursor)
            val state = viewModel.uiState.value
            assertEquals(listOf("a", "b", "c"), state.items.map { it.challengeId })
            assertNull(state.nextCursor)
            assertFalse(state.isLoadingMore)
        }

    @Test
    fun `마지막 페이지에 닿으면 하단에 다다라도 더 조회하지 않는다`() =
        runTest {
            val repository = FakeExploreRepository(listOf(Result.success(exploreResult("a", nextCursor = null))))
            val viewModel = viewModel(repository = repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals(1, repository.queries.size)
        }

    @Test
    fun `다음 페이지를 부르는 중이면 하단 신호가 겹쳐 와도 한 번만 조회한다`() =
        runTest {
            // 스크롤 한 번에 하단 신호가 여러 번 올라온다. 막지 않으면 같은 커서로 중복 페이지가 붙는다.
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult("a", nextCursor = "cursor-1")),
                        Result.success(exploreResult("b", nextCursor = null)),
                    ),
                )
            val viewModel = viewModel(repository = repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            val inFlight = CompletableDeferred<Unit>()
            repository.gate = inFlight

            viewModel.onIntent(ExploreListIntent.LoadMore)
            viewModel.onIntent(ExploreListIntent.LoadMore)
            inFlight.complete(Unit)

            assertEquals(2, repository.queries.size)
            assertEquals(listOf("a", "b"), viewModel.uiState.value.items.map { it.challengeId })
        }

    @Test
    fun `다음 페이지가 실패하면 이미 본 목록은 그대로 두고 하단 재시도만 켠다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult("a", "b", nextCursor = "cursor-1")),
                        Result.failure(RuntimeException("네트워크가 불안정해요")),
                    ),
                )
            val viewModel = viewModel(repository = repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            val state = viewModel.uiState.value
            assertEquals(listOf("a", "b"), state.items.map { it.challengeId })
            assertTrue(state.loadMoreFailed)
            assertFalse(state.isLoadingMore)
            // 전면 오류 문구를 띄우면 이미 읽던 목록이 사라진 것처럼 보인다.
            assertNull(state.errorMessage)
        }

    @Test
    fun `이어붙이는 중 커서가 상하면 재시도를 띄우지 않고 첫 페이지부터 다시 받는다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult("a", "b", nextCursor = "cursor-1")),
                        Result.failure(CursorInvalidException()),
                        Result.success(exploreResult("a", "b", nextCursor = "cursor-2")),
                    ),
                )
            val viewModel = viewModel(repository = repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertNull(repository.queries.last().cursor)
            val state = viewModel.uiState.value
            assertEquals(listOf("a", "b"), state.items.map { it.challengeId })
            assertFalse(state.loadMoreFailed)
            assertNull(state.errorMessage)
        }

    @Test
    fun `페이지를 이어 붙일 때마다 스크롤 깊이 이벤트의 페이지 번호가 오른다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult("a", nextCursor = "cursor-1")),
                        Result.success(exploreResult("b", nextCursor = "cursor-2")),
                        Result.success(exploreResult("c", nextCursor = null)),
                    ),
                )
            val sink = RecordingSink()
            val viewModel = viewModel(repository = repository, observability = testObservability(sink = sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)
            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals(
                listOf(AttrValue.Int64(1), AttrValue.Int64(2)),
                sink.attrsOf("explore_list_load_more", "page_index"),
            )
        }

    // ---------- 조건 변경 ----------

    @Test
    fun `필터를 적용하면 첫 페이지부터 다시 조회하고 결과 수와 함께 기록한다`() =
        runTest {
            // result_count 는 응답이 와야 알 수 있어 인텐트 시점이 아니라 결과 시점에 나간다.
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult("a", "b", nextCursor = "cursor-1")),
                        Result.success(exploreResult("c")),
                    ),
                )
            val sink = RecordingSink()
            val viewModel = viewModel(repository = repository, observability = testObservability(sink = sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            val filter = ExploreFilter(categories = setOf(Category.READING), eligibleOnly = true)

            viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))

            val query = repository.queries.last()
            assertEquals(filter, query.filter)
            assertNull(query.cursor)
            assertEquals(filter, viewModel.uiState.value.filter)
            assertEquals(AttrValue.Int64(1), sink.attrsOf("explore_filter_apply", "result_count").single())
        }

    @Test
    fun `필터 결과가 0건이면 빈 결과를 필터 적용과 별개로 한 번 더 기록한다`() =
        runTest {
            // 필터 사용률과 빈 결과율은 분모가 달라 하나로 합치면 가드레일을 계산할 수 없다 — 중복 전송이 정상이다.
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult("a")),
                        Result.success(exploreResult()),
                    ),
                )
            val sink = RecordingSink()
            val viewModel = viewModel(repository = repository, observability = testObservability(sink = sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(eligibleOnly = true)))

            assertEquals(1, sink.eventNames.count { it == "explore_filter_apply" })
            assertEquals(1, sink.eventNames.count { it == "explore_empty_result" })
        }

    @Test
    fun `정렬을 바꾸면 이전 정렬과 함께 기록하고 첫 페이지부터 다시 조회한다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    listOf(
                        Result.success(exploreResult("a", nextCursor = "cursor-1")),
                        Result.success(exploreResult("b")),
                    ),
                )
            val sink = RecordingSink()
            val viewModel = viewModel(repository = repository, observability = testObservability(sink = sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.RECENT))

            val query = repository.queries.last()
            assertEquals(ExploreSort.RECENT, query.sort)
            assertNull(query.cursor)
            assertEquals(listOf("b"), viewModel.uiState.value.items.map { it.challengeId })
            assertEquals(AttrValue.Str("POPULAR"), sink.attrsOf("explore_sort_change", "sort_from").single())
            assertEquals(AttrValue.Str("RECENT"), sink.attrsOf("explore_sort_change", "sort_to").single())
        }

    @Test
    fun `티어 조건 끄기는 나머지 필터를 유지한 채 그 조건만 풀고 다시 조회한다`() =
        runTest {
            // 빈 결과 화면의 CTA 다. 같이 걸어둔 카테고리까지 풀어버리면 사용자가 고른 맥락이 사라진다.
            val repository = FakeExploreRepository()
            val viewModel = viewModel(repository = repository)
            val filter = ExploreFilter(categories = setOf(Category.READING), eligibleOnly = true)
            viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))

            viewModel.onIntent(ExploreListIntent.ClearEligibleOnly)

            assertEquals(filter.copy(eligibleOnly = false), repository.queries.last().filter)
            assertNull(repository.queries.last().cursor)
        }

    // ---------- 노출·클릭 ----------

    @Test
    fun `같은 카드가 스크롤로 다시 보여도 노출은 한 번만 기록한다`() =
        runTest {
            // 노출이 부풀면 상세 진입률(클릭/노출)이 실제보다 낮게 나온다.
            val repository = FakeExploreRepository(listOf(Result.success(exploreResult("a", "b"))))
            val sink = RecordingSink()
            val viewModel = viewModel(repository = repository, observability = testObservability(sink = sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.CardImpression("a"))
            viewModel.onIntent(ExploreListIntent.CardImpression("a"))

            assertEquals(1, sink.eventNames.count { it == "challenge_card_impression" })
            assertEquals(AttrValue.Int64(0), sink.attrsOf("challenge_card_impression", "position").single())
        }

    @Test
    fun `목록에 없는 카드의 노출은 기록하지 않는다`() =
        runTest {
            val repository = FakeExploreRepository(listOf(Result.success(exploreResult("a"))))
            val sink = RecordingSink()
            val viewModel = viewModel(repository = repository, observability = testObservability(sink = sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.CardImpression("사라진-카드"))

            assertTrue(sink.eventNames.none { it == "challenge_card_impression" })
        }

    @Test
    fun `조건이 바뀌어 목록을 다시 그리면 같은 카드의 노출을 다시 기록한다`() =
        runTest {
            // 새 목록에서의 노출은 다른 사건이다 — 위치도 정렬도 달라진다.
            val repository = FakeExploreRepository(listOf(Result.success(exploreResult("a"))))
            val sink = RecordingSink()
            val viewModel = viewModel(repository = repository, observability = testObservability(sink = sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            viewModel.onIntent(ExploreListIntent.CardImpression("a"))

            viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.RECENT))
            viewModel.onIntent(ExploreListIntent.CardImpression("a"))

            assertEquals(2, sink.eventNames.count { it == "challenge_card_impression" })
        }

    @Test
    fun `카드를 누르면 목록에서의 위치와 함께 기록하고 그 챌린지 상세로 이동한다`() =
        runTest {
            val repository = FakeExploreRepository(listOf(Result.success(exploreResult("a", "b"))))
            val sink = RecordingSink()
            val navigation = RecordingNavigationHelper()
            val viewModel =
                viewModel(
                    repository = repository,
                    navigation = navigation,
                    observability = testObservability(sink = sink),
                )
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.OpenChallenge("b"))

            assertEquals(ChallengeDetailPage("b").toRoute(), navigation.routes.single())
            assertEquals(AttrValue.Int64(1), sink.attrsOf("challenge_card_click", "position").single())
            assertEquals(AttrValue.Str("list"), sink.attrsOf("challenge_card_click", "source").single())
        }

    @Test
    fun `뒤로가기는 새 화면을 쌓지 않고 이전 화면으로 돌아간다`() =
        runTest {
            val navigation = RecordingNavigationHelper()
            val viewModel = viewModel(navigation = navigation)

            viewModel.onIntent(ExploreListIntent.Back)

            assertEquals(1, navigation.backCount)
            assertTrue(navigation.routes.isEmpty())
            assertTrue(navigation.pages.isEmpty())
        }
}

// 협력자 기본값을 한 곳에 모아, 테스트마다 흔드는 축만 호출부에 드러나게 한다.
private fun viewModel(
    repository: ExploreRepository = FakeExploreRepository(),
    navigation: NavigationHelper = RecordingNavigationHelper(),
    observability: Observability = testObservability(),
) = ExploreListViewModel(
    exploreRepository = repository,
    navigationHelper = navigation,
    observability = observability,
)

private val RecordingSink.eventNames: List<String>
    get() = payloads.filterIsInstance<BusinessPayload.Custom>().map { it.name }

/** [event] 로 나간 이벤트들의 [key] 값을 발생 순서대로. 개수까지 같이 볼 수 있어야 중복 전송을 잡는다. */
private fun RecordingSink.attrsOf(
    event: String,
    key: String,
): List<AttrValue> =
    payloads
        .filterIsInstance<BusinessPayload.Custom>()
        .filter { it.name == event }
        .mapNotNull { it.attrs.entries[AttrKey(key)] }

/**
 * 인텐트마다 그것을 지키는 테스트 이름. 인텐트가 늘면 이 `when` 이 컴파일되지 않아 누락이 드러난다 —
 * "모든 인텐트를 덮었다"를 사람의 기억이 아니라 컴파일러가 보증하게 하는 장치다.
 */
private fun ExploreListIntent.coveredBy(): List<String> =
    when (this) {
        is ExploreListIntent.Load ->
            listOf(
                "카테고리 타일로 들어오면 그 카테고리를 걸고 지정된 정렬로 첫 페이지를 조회한다",
                "모르는 카테고리·정렬로 들어오면 전체 목록을 기본 정렬로 조회한다",
                "이미 목록을 불러왔으면 다시 진입해도 조회하지도 진입을 다시 세지도 않는다",
                "카테고리로 들어온 진입과 전체 진입을 진입 이벤트가 구분한다",
            )
        ExploreListIntent.LoadMore ->
            listOf(
                "커서가 남아 있으면 다음 페이지를 목록 뒤에 이어 붙인다",
                "마지막 페이지에 닿으면 하단에 다다라도 더 조회하지 않는다",
                "다음 페이지를 부르는 중이면 하단 신호가 겹쳐 와도 한 번만 조회한다",
                "다음 페이지가 실패하면 이미 본 목록은 그대로 두고 하단 재시도만 켠다",
                "이어붙이는 중 커서가 상하면 재시도를 띄우지 않고 첫 페이지부터 다시 받는다",
                "페이지를 이어 붙일 때마다 스크롤 깊이 이벤트의 페이지 번호가 오른다",
            )
        is ExploreListIntent.ApplyFilter ->
            listOf(
                "필터를 적용하면 첫 페이지부터 다시 조회하고 결과 수와 함께 기록한다",
                "필터 결과가 0건이면 빈 결과를 필터 적용과 별개로 한 번 더 기록한다",
            )
        is ExploreListIntent.SelectSort -> listOf("정렬을 바꾸면 이전 정렬과 함께 기록하고 첫 페이지부터 다시 조회한다")
        ExploreListIntent.ClearEligibleOnly -> listOf("티어 조건 끄기는 나머지 필터를 유지한 채 그 조건만 풀고 다시 조회한다")
        is ExploreListIntent.CardImpression ->
            listOf(
                "같은 카드가 스크롤로 다시 보여도 노출은 한 번만 기록한다",
                "목록에 없는 카드의 노출은 기록하지 않는다",
                "조건이 바뀌어 목록을 다시 그리면 같은 카드의 노출을 다시 기록한다",
            )
        is ExploreListIntent.OpenChallenge -> listOf("카드를 누르면 목록에서의 위치와 함께 기록하고 그 챌린지 상세로 이동한다")
        ExploreListIntent.Back -> listOf("뒤로가기는 새 화면을 쌓지 않고 이전 화면으로 돌아간다")
    }
