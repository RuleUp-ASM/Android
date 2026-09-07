package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.card
import com.ruleup.challenge.presentation.fake.fails
import com.ruleup.challenge.presentation.fake.page
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.model.AttrKey
import com.ruleup.observability.domain.model.AttrValue
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 둘러보기 ViewModel — 인텐트가 서버 조회·상태·이동·지표로 옮겨지는 **전이**만 본다.
 *
 * 필터·정렬 값 자체의 규칙(6종 정렬, 활성 필터 수, csv 직렬화)은 `ExploreTest` 가, 빈 결과 문구와
 * 더 부를 수 있는지는 `ExploreListStateTest` 가 이미 끝냈다. 여기서 다시 세지 않는다.
 *
 * 이 화면은 실패를 사용자에게 되묻지 않고 **스스로 조건을 고쳐 다시 조회**하므로, 한 인텐트가 서버에
 * 몇 번 어떤 조건으로 나갔는지가 곧 계약이다. 그래서 단언이 상태뿐 아니라 호출 기록에도 걸린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreListViewModelTest {
    @BeforeTest
    fun setUp() {
        // viewModelScope 는 Dispatchers.Main 을 쓴다. Unconfined 라 launch 가 그 자리에서 끝나 상태를 바로 읽을 수 있다.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------- 진입 ----------

    @Test
    fun `카테고리 타일로 들어오면 그 카테고리를 필터에 걸고 조회한다`() =
        runTest {
            val repository = FakeExploreRepository(page(card("c1")))
            val sink = RecordingSink()
            val viewModel = viewModel(repository, observability = testObservability(sink))

            viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))

            assertEquals(setOf(Category.EXERCISE), repository.calls.single().filter.categories)
            assertEquals(setOf(Category.EXERCISE), viewModel.uiState.value.filter.categories)
            assertEquals(AttrValue.Str("category"), sink.event("explore_list_view").attr("entry"))
        }

    @Test
    fun `모르는 카테고리나 정렬로 들어와도 전체 목록을 기본 정렬로 연다`() =
        runTest {
            // 라우트 인자는 딥링크·구버전 링크에서도 오므로 서버 정의 밖의 값이 그대로 들어올 수 있다.
            val repository = FakeExploreRepository(page(card("c1")))
            val sink = RecordingSink()
            val viewModel = viewModel(repository, observability = testObservability(sink))

            viewModel.onIntent(ExploreListIntent.Load(category = "NOT_A_CATEGORY", sort = "TEMPLATE_USAGE"))

            assertEquals(ExploreFilter.none, repository.calls.single().filter)
            assertEquals(ExploreSort.POPULAR, repository.calls.single().sort)
            assertEquals(AttrValue.Str("all"), sink.event("explore_list_view").attr("entry"))
        }

    @Test
    fun `화면이 다시 붙어도 진입 조회와 진입 이벤트는 한 번뿐이다`() =
        runTest {
            // 진입 이벤트는 탐색 전환율의 분모다. 두 번 나가면 전환율이 절반으로 보인다.
            val repository = FakeExploreRepository(page(card("c1")))
            val sink = RecordingSink()
            val viewModel = viewModel(repository, observability = testObservability(sink))

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.calls.size)
            assertEquals(1, sink.customEvents("explore_list_view").size)
        }

    // ---------- 첫 페이지 ----------

    @Test
    fun `첫 페이지를 받으면 로딩을 끝내고 다음 커서를 들고 있는다`() =
        runTest {
            val viewModel = viewModel(FakeExploreRepository(page(card("c1"), card("c2"), nextCursor = "cur1")))

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(listOf("c1", "c2"), state.items.map { it.challengeId })
            assertEquals("cur1", state.nextCursor)
        }

    @Test
    fun `첫 페이지가 실패하면 목록을 비운 채 사유를 보여준다`() =
        runTest {
            val viewModel = viewModel(FakeExploreRepository(fails(RuntimeException("네트워크가 불안정해요"))))

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.items.isEmpty())
            assertEquals("네트워크가 불안정해요", state.errorMessage)
        }

    @Test
    fun `실패에 사유가 실려 오지 않아도 빈 화면을 그대로 두지 않는다`() =
        runTest {
            // message 가 null 인 예외는 흔하다. 그때 errorMessage 가 null 이면 화면이 아무 말도 없이 비어 버린다.
            val viewModel = viewModel(FakeExploreRepository(fails(RuntimeException())))

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `결과가 0건이면 빈 결과를 따로 기록한다`() =
        runTest {
            // 빈 결과율 가드레일(10% 이하)의 분자다. 필터 이벤트와 분모가 달라 합칠 수 없다.
            val sink = RecordingSink()
            val viewModel = viewModel(FakeExploreRepository(page()), observability = testObservability(sink))

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, sink.customEvents("explore_empty_result").size)
        }

    // ---------- 이어 붙이기 ----------

    @Test
    fun `다음 페이지는 커서를 실어 부르고 기존 목록 뒤에 이어 붙인다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    page(card("c1"), nextCursor = "cur1"),
                    page(card("c2")),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals("cur1", repository.calls[1].cursor)
            assertEquals(listOf("c1", "c2"), viewModel.uiState.value.items.map { it.challengeId })
            assertNull(viewModel.uiState.value.nextCursor)
            assertFalse(viewModel.uiState.value.isLoadingMore)
        }

    @Test
    fun `마지막 페이지에 닿으면 더 부르지 않는다`() =
        runTest {
            // 커서가 없는데도 부르면 목록 하단에서 같은 페이지를 무한히 재요청한다.
            val repository = FakeExploreRepository(page(card("c1")))
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals(1, repository.calls.size)
        }

    @Test
    fun `다음 페이지가 실패해도 이미 보고 있던 목록은 지우지 않는다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    page(card("c1"), nextCursor = "cur1"),
                    fails(RuntimeException("서버 오류")),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            val state = viewModel.uiState.value
            assertEquals(listOf("c1"), state.items.map { it.challengeId })
            assertTrue(state.loadMoreFailed)
            assertFalse(state.isLoadingMore)
            // 전면 에러로 번지면 사용자가 보고 있던 목록이 통째로 사라진다.
            assertNull(state.errorMessage)
        }

    @Test
    fun `이어 붙인 페이지마다 스크롤 깊이가 하나씩 올라간다`() =
        runTest {
            // 깊이가 늘 0이면 "몇 페이지까지 보는가"를 못 읽어 목록 길이 정책을 정할 수 없다.
            val repository =
                FakeExploreRepository(
                    page(card("c1"), nextCursor = "cur1"),
                    page(card("c2"), nextCursor = "cur2"),
                    page(card("c3")),
                )
            val sink = RecordingSink()
            val viewModel = viewModel(repository, observability = testObservability(sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)
            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertEquals(
                listOf(AttrValue.Int64(1), AttrValue.Int64(2)),
                sink.customEvents("explore_list_load_more").map { it.attr("page_index") },
            )
        }

    // ---------- 필터·정렬 ----------

    @Test
    fun `필터를 적용하면 커서를 버리고 첫 페이지부터 다시 받는다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    page(card("c1"), nextCursor = "cur1"),
                    page(card("c2"), card("c3")),
                )
            val sink = RecordingSink()
            val viewModel = viewModel(repository, observability = testObservability(sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.STUDY))))

            assertNull(repository.calls[1].cursor)
            assertEquals(setOf(Category.STUDY), repository.calls[1].filter.categories)
            assertEquals(listOf("c2", "c3"), viewModel.uiState.value.items.map { it.challengeId })
            // 결과 수는 응답이 와야 알 수 있어 인텐트 시점이 아니라 여기서 실린다.
            assertEquals(AttrValue.Int64(2), sink.event("explore_filter_apply").attr("result_count"))
        }

    @Test
    fun `정렬을 바꾸면 어디서 어디로 옮겼는지까지 남기고 다시 받는다`() =
        runTest {
            val repository = FakeExploreRepository(page(card("c1")), page(card("c2")))
            val sink = RecordingSink()
            val viewModel = viewModel(repository, observability = testObservability(sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.DEADLINE))

            assertEquals(ExploreSort.DEADLINE, repository.calls[1].sort)
            assertEquals(ExploreSort.DEADLINE, viewModel.uiState.value.sort)
            val changed = sink.event("explore_sort_change")
            assertEquals(AttrValue.Str("POPULAR"), changed.attr("sort_from"))
            assertEquals(AttrValue.Str("DEADLINE"), changed.attr("sort_to"))
        }

    @Test
    fun `티어 조건을 끌 때 나머지 필터는 건드리지 않는다`() =
        runTest {
            // 빈 결과에서 제안하는 완화는 티어 하나뿐이다. 카테고리까지 풀리면 사용자가 고른 조건이 말없이 사라진다.
            val applied = ExploreFilter(categories = setOf(Category.STUDY), eligibleOnly = true)
            val repository = FakeExploreRepository(page(), page(), page(card("c1")))
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            viewModel.onIntent(ExploreListIntent.ApplyFilter(applied))

            viewModel.onIntent(ExploreListIntent.ClearEligibleOnly)

            assertEquals(applied.copy(eligibleOnly = false), repository.calls[2].filter)
            assertEquals(applied.copy(eligibleOnly = false), viewModel.uiState.value.filter)
        }

    // ---------- 서버가 조건을 거절할 때 ----------

    @Test
    fun `서버가 정렬을 거절하면 되묻지 않고 기본 정렬로 다시 받는다`() =
        runTest {
            val repository = FakeExploreRepository(fails(InvalidSortTypeException()), page(card("c1")))
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = "DEADLINE"))

            assertEquals(listOf(ExploreSort.DEADLINE, ExploreSort.POPULAR), repository.calls.map { it.sort })
            assertEquals(ExploreSort.POPULAR, viewModel.uiState.value.sort)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `기본 정렬마저 거절당하면 되돌릴 곳이 없어 실패로 알린다`() =
        runTest {
            // 되돌릴 곳이 없는데도 재조회하면 같은 요청을 무한히 반복한다.
            val repository = FakeExploreRepository(fails(InvalidSortTypeException()))
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.calls.size)
            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `서버가 필터를 거절하면 필터를 비우고 다시 받는다`() =
        runTest {
            val repository = FakeExploreRepository(fails(InvalidFilterValueException()), page(card("c1")))
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))

            assertEquals(ExploreFilter.none, repository.calls[1].filter)
            assertEquals(ExploreFilter.none, viewModel.uiState.value.filter)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `필터가 이미 비었는데도 거절당하면 실패로 알린다`() =
        runTest {
            val repository = FakeExploreRepository(fails(InvalidFilterValueException()))
            val viewModel = viewModel(repository)

            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            assertEquals(1, repository.calls.size)
            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `커서가 상하면 사용자에게 알리지 않고 첫 페이지부터 다시 받는다`() =
        runTest {
            val repository =
                FakeExploreRepository(
                    page(card("c1"), nextCursor = "cur1"),
                    fails(CursorInvalidException()),
                    page(card("c1"), card("c2")),
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.LoadMore)

            assertNull(repository.calls[2].cursor)
            val state = viewModel.uiState.value
            assertEquals(listOf("c1", "c2"), state.items.map { it.challengeId })
            assertFalse(state.loadMoreFailed)
            assertNull(state.errorMessage)
        }

    // ---------- 노출·클릭·뒤로 ----------

    @Test
    fun `같은 카드가 다시 보여도 노출은 한 번만 센다`() =
        runTest {
            // 스크롤로 되돌아온 카드를 다시 세면 노출이 부풀어 상세 진입률이 실제보다 낮게 나온다.
            val sink = RecordingSink()
            val viewModel = viewModel(FakeExploreRepository(page(card("c1"))), observability = testObservability(sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.CardImpression("c1"))
            viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

            val impression = sink.event("challenge_card_impression")
            assertEquals(AttrValue.Int64(0), impression.attr("position"))
            assertEquals(AttrValue.Bool(true), impression.attr("has_metrics"))
        }

    @Test
    fun `조건이 바뀌어 목록이 갈리면 같은 카드도 새 노출로 센다`() =
        runTest {
            // 필터가 바뀌면 순위가 달라진다. 이전 목록의 노출 기록을 들고 있으면 새 목록의 노출이 통째로 사라진다.
            val sink = RecordingSink()
            val repository = FakeExploreRepository(page(card("c1")), page(card("c1")))
            val viewModel = viewModel(repository, observability = testObservability(sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
            viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

            viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.STUDY))))
            viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

            assertEquals(2, sink.customEvents("challenge_card_impression").size)
        }

    @Test
    fun `목록에 없는 카드는 노출로 세지 않는다`() =
        runTest {
            val sink = RecordingSink()
            val viewModel = viewModel(FakeExploreRepository(page(card("c1"))), observability = testObservability(sink))
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.CardImpression("사라진-카드"))

            assertTrue(sink.customEvents("challenge_card_impression").isEmpty())
        }

    @Test
    fun `카드를 누르면 목록에서의 위치와 함께 남기고 상세로 보낸다`() =
        runTest {
            // 노출·클릭·상세를 잇는 건 challenge_id 와 position 이다. 하나만 어긋나도 진입률이 계산되지 않는다.
            val nav = RecordingNavigationHelper()
            val sink = RecordingSink()
            val viewModel =
                viewModel(
                    FakeExploreRepository(page(card("c1"), card("c2"))),
                    nav = nav,
                    observability = testObservability(sink),
                )
            viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

            viewModel.onIntent(ExploreListIntent.OpenChallenge("c2"))

            assertEquals(ChallengeDetailPage("c2").toRoute(), nav.routes.single())
            val click = sink.event("challenge_card_click")
            assertEquals(AttrValue.Int64(1), click.attr("position"))
            assertEquals(AttrValue.Str("list"), click.attr("source"))
        }

    @Test
    fun `뒤로는 이전 화면으로 돌려보내고 새 화면을 쌓지 않는다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(nav = nav)

            viewModel.onIntent(ExploreListIntent.Back)

            assertEquals(1, nav.backCount)
            assertTrue(nav.routes.isEmpty())
        }

    // ---------- 열거의 근거 ----------

    @Test
    fun `모든 인텐트가 이 파일에서 한 번은 다뤄진다`() =
        runTest {
            // 인텐트가 늘면 coveredBy 의 when 이 컴파일에서 깨진다 — 검증 없이 새 인텐트가 들어오는 걸 막는 장치다.
            val everyIntent =
                listOf(
                    ExploreListIntent.Load(category = null, sort = null),
                    ExploreListIntent.LoadMore,
                    ExploreListIntent.ApplyFilter(ExploreFilter.none),
                    ExploreListIntent.SelectSort(ExploreSort.RECENT),
                    ExploreListIntent.ClearEligibleOnly,
                    ExploreListIntent.CardImpression("c1"),
                    ExploreListIntent.OpenChallenge("c1"),
                    ExploreListIntent.Back,
                )

            assertTrue(everyIntent.all { it.coveredBy().isNotBlank() })
        }
}

/** 이 인텐트를 지키는 테스트. 인텐트가 늘면 여기서 컴파일이 깨지고, 빈 문자열은 구멍이라는 뜻이다. */
private fun ExploreListIntent.coveredBy(): String =
    when (this) {
        is ExploreListIntent.Load -> "카테고리 타일로 들어오면 그 카테고리를 필터에 걸고 조회한다"
        ExploreListIntent.LoadMore -> "다음 페이지는 커서를 실어 부르고 기존 목록 뒤에 이어 붙인다"
        is ExploreListIntent.ApplyFilter -> "필터를 적용하면 커서를 버리고 첫 페이지부터 다시 받는다"
        is ExploreListIntent.SelectSort -> "정렬을 바꾸면 어디서 어디로 옮겼는지까지 남기고 다시 받는다"
        ExploreListIntent.ClearEligibleOnly -> "티어 조건을 끌 때 나머지 필터는 건드리지 않는다"
        is ExploreListIntent.CardImpression -> "같은 카드가 다시 보여도 노출은 한 번만 센다"
        is ExploreListIntent.OpenChallenge -> "카드를 누르면 목록에서의 위치와 함께 남기고 상세로 보낸다"
        ExploreListIntent.Back -> "뒤로는 이전 화면으로 돌려보내고 새 화면을 쌓지 않는다"
    }

/** 협력자 기본값을 한곳에 모아, 테스트마다 흔드는 축만 인자로 드러나게 한다. */
private fun viewModel(
    repository: ExploreRepository = FakeExploreRepository(page()),
    nav: NavigationHelper = RecordingNavigationHelper(),
    observability: Observability = testObservability(),
) = ExploreListViewModel(repository, nav, observability)

private fun RecordingSink.customEvents(name: String): List<BusinessPayload.Custom> =
    payloads.filterIsInstance<BusinessPayload.Custom>().filter { it.name == name }

private fun RecordingSink.event(name: String): BusinessPayload.Custom = customEvents(name).single()

private fun BusinessPayload.Custom.attr(key: String): AttrValue? = attrs.entries[AttrKey(key)]
