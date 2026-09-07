package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.presentation.MainDispatcherRule
import com.ruleup.challenge.presentation.fake.ExploreCall
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.RecordingNavigationHelper
import com.ruleup.challenge.presentation.fake.answer
import com.ruleup.challenge.presentation.fake.challenges
import com.ruleup.challenge.presentation.fake.exploreChallenge
import com.ruleup.challenge.presentation.fake.throws
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.model.AttrKey
import com.ruleup.observability.domain.model.AttrValue
import com.ruleup.observability.domain.test.RecordingSink
import com.ruleup.observability.domain.test.testObservability
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 둘러보기 목록 ViewModel.
 *
 * 이 화면의 어려운 부분은 목록을 그리는 게 아니라 **서버가 조건을 거절했을 때 스스로 고쳐 다시 묻는
 * 경로**와, **전환율 계산에 쓰이는 이벤트가 정확히 한 번씩 나가는지**다. 둘 다 화면을 띄워서는
 * 확인할 수 없어 인텐트 → 상태·조회 조건·이벤트로 검증한다.
 *
 * 관측은 목이 아니라 실제 `Observability` 에 [RecordingSink] 를 꽂아 본다 — 게이트와 조립이 실제로
 * 도는 상태여야 채널·이름·값 타입까지 계약대로인지 드러난다.
 */
class ExploreListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExploreRepository()
    private val navigation = RecordingNavigationHelper()
    private val sink = RecordingSink()

    private val viewModel =
        ExploreListViewModel(
            exploreRepository = repository,
            navigationHelper = navigation,
            observability = testObservability(sink = sink),
        )

    private val state get() = viewModel.uiState.value

    private val events get() = sink.payloads.filterIsInstance<BusinessPayload.Custom>()

    private fun names() = events.map { it.name }

    private fun eventsNamed(name: String) = events.filter { it.name == name }

    private fun lastEvent(name: String) = eventsNamed(name).last()

    private fun BusinessPayload.Custom.attr(key: String): AttrValue? = attrs.entries[AttrKey(key)]

    private fun itemIds() = state.items.map { it.challengeId }

    /** 목록이 이미 한 페이지 떠 있는 상태를 만든다. 필터·정렬·페이지네이션 테스트의 공통 출발점이다. */
    private fun loaded(
        items: List<String> = listOf("a", "b", "c"),
        nextCursor: String? = null,
    ) {
        repository.enqueue(answer(challenges(*items.toTypedArray()), nextCursor))
        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
        sink.clear()
    }

    // ---------- 진입 ----------

    @Test
    fun `카테고리로 들어오면 그 카테고리를 필터에 깔고 첫 페이지를 받는다`() {
        repository.enqueue(answer(challenges("a"), nextCursor = "cur1"))

        viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = "RECENT"))

        assertEquals(
            ExploreCall(ExploreFilter(categories = setOf(Category.EXERCISE)), ExploreSort.RECENT, null),
            repository.calls.single(),
        )
        assertEquals(listOf("a"), itemIds())
        assertEquals("cur1", state.nextCursor)
        assertFalse(state.isLoading)
        assertEquals(AttrValue.Str("category"), lastEvent("explore_list_view").attr("entry"))
    }

    @Test
    fun `모르는 카테고리·정렬 인자는 전체 목록과 기본 정렬로 떨어진다`() {
        // 라우트 인자는 딥링크·구버전 화면에서도 들어온다. 서버 정의 밖 값이 그대로 나가면 400 이다.
        viewModel.onIntent(ExploreListIntent.Load(category = "TIDYING", sort = "TEMPLATE_USAGE"))

        assertEquals(
            ExploreCall(ExploreFilter.none, ExploreSort.POPULAR, null),
            repository.calls.single(),
        )
        assertEquals(AttrValue.Str("all"), lastEvent("explore_list_view").attr("entry"))
    }

    @Test
    fun `재진입해도 첫 페이지를 다시 받지 않는다`() {
        // 화면 회전·재구성마다 Load 가 다시 올라온다. 막지 않으면 목록이 깜빡이고 진입 이벤트가 부푼다.
        repository.enqueue(answer(challenges("a")))

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertEquals(1, repository.calls.size)
        assertEquals(1, eventsNamed("explore_list_view").size)
    }

    // ---------- 필터·정렬 ----------

    @Test
    fun `필터를 적용하면 커서를 버리고 첫 페이지부터 다시 받는다`() {
        loaded(nextCursor = "cur1")
        val filter = ExploreFilter(categories = setOf(Category.READING), verifyType = VerificationType.MANUAL)
        repository.enqueue(answer(challenges("x", "y")))

        viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))

        assertEquals(ExploreCall(filter, ExploreSort.POPULAR, null), repository.calls.last())
        assertEquals(filter, state.filter)
        assertEquals(listOf("x", "y"), itemIds())
        assertNull(state.nextCursor)
        assertEquals(AttrValue.Int64(2), lastEvent("explore_filter_apply").attr("result_count"))
    }

    @Test
    fun `빈 결과는 필터 이벤트와 빈 결과 이벤트를 함께 남긴다`() {
        // 분모가 달라서(필터 사용률 vs 빈 결과율) 하나로 합칠 수 없다 — 중복 전송이 정상이다.
        loaded()
        repository.enqueue(answer(items = emptyList()))

        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(eligibleOnly = true)))

        assertEquals(listOf("explore_filter_apply", "explore_empty_result"), names())
        assertEquals(AttrValue.Int64(0), lastEvent("explore_filter_apply").attr("result_count"))
    }

    @Test
    fun `정렬을 바꾸면 어디서 어디로 옮겼는지와 결과 수를 함께 남긴다`() {
        loaded()
        repository.enqueue(answer(challenges("x")))

        viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.DEADLINE))

        assertEquals(ExploreSort.DEADLINE, state.sort)
        val changed = lastEvent("explore_sort_change")
        assertEquals(AttrValue.Str("POPULAR"), changed.attr("sort_from"))
        assertEquals(AttrValue.Str("DEADLINE"), changed.attr("sort_to"))
        assertEquals(AttrValue.Int64(1), changed.attr("result_count"))
    }

    @Test
    fun `티어 조건만 끄고 나머지 필터는 그대로 둔다`() {
        // 빈 결과에서 "티어 조건 끄기"는 완화 제안이다. 카테고리까지 날리면 사용자가 고른 걸 잃는다.
        val filter =
            ExploreFilter(
                categories = setOf(Category.STUDY),
                verifyType = VerificationType.AUTO,
                eligibleOnly = true,
            )
        loaded()
        viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))
        sink.clear()

        viewModel.onIntent(ExploreListIntent.ClearEligibleOnly)

        assertEquals(filter.copy(eligibleOnly = false), state.filter)
        assertEquals(filter.copy(eligibleOnly = false), repository.calls.last().filter)
        // 사용자가 필터 시트에서 "적용"을 누른 게 아니므로 필터 사용률에 세지 않는다.
        assertFalse(names().contains("explore_filter_apply"))
    }

    // ---------- 페이지네이션 ----------

    @Test
    fun `다음 페이지는 받은 커서로 이어 붙인다`() {
        loaded(items = listOf("a", "b"), nextCursor = "cur1")
        repository.enqueue(answer(challenges("c"), nextCursor = "cur2"))

        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(ExploreCall(ExploreFilter.none, ExploreSort.POPULAR, "cur1"), repository.calls.last())
        assertEquals(listOf("a", "b", "c"), itemIds())
        assertEquals("cur2", state.nextCursor)
        assertFalse(state.isLoadingMore)
        assertEquals(AttrValue.Int64(1), lastEvent("explore_list_load_more").attr("page_index"))
    }

    @Test
    fun `마지막 페이지에서는 더 요청하지 않는다`() {
        loaded(nextCursor = null)

        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(1, repository.calls.size)
    }

    @Test
    fun `앞선 요청이 끝나기 전의 추가 요청은 무시한다`() {
        // 하단 근접 판정은 스크롤마다 올라온다. 막지 않으면 같은 커서로 여러 번 나가 중복이 붙는다.
        loaded(nextCursor = "cur1")
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        repository.alwaysAnswer(answer(challenges("c")))

        viewModel.onIntent(ExploreListIntent.LoadMore)
        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertTrue(state.isLoadingMore)
        assertEquals(2, repository.calls.size)
        gate.complete(Unit)
        assertEquals(listOf("a", "b", "c", "c"), itemIds())
    }

    @Test
    fun `다음 페이지가 실패해도 이미 받은 목록은 지우지 않는다`() {
        loaded(items = listOf("a", "b"), nextCursor = "cur1")
        repository.enqueue(throws(RuntimeException("네트워크")))

        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(listOf("a", "b"), itemIds())
        assertTrue(state.loadMoreFailed)
        assertFalse(state.isLoadingMore)
        assertEquals("cur1", state.nextCursor)
        // 전면 오류가 아니다 — 하단 재시도로만 안내한다.
        assertNull(state.errorMessage)
    }

    @Test
    fun `다음 페이지 커서가 상하면 조용히 첫 페이지부터 다시 받는다`() {
        loaded(items = listOf("a", "b"), nextCursor = "cur1")
        repository.enqueue(throws(CursorInvalidException()), answer(challenges("z")))

        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(listOf("z"), itemIds())
        assertFalse(state.loadMoreFailed)
        assertNull(state.errorMessage)
        assertEquals(3, repository.calls.size)
    }

    @Test
    fun `필터를 바꾸면 스크롤 깊이를 처음부터 다시 센다`() {
        loaded(nextCursor = "cur1")
        repository.enqueue(answer(challenges("d"), nextCursor = "cur2"))
        viewModel.onIntent(ExploreListIntent.LoadMore)
        repository.enqueue(answer(challenges("e"), nextCursor = "cur3"))
        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.MIND))))
        repository.enqueue(answer(challenges("f")))

        viewModel.onIntent(ExploreListIntent.LoadMore)

        // 같은 값이 두 번 나와야 한다. 이어서 세면 목록이 바뀐 뒤의 깊이가 앞 목록에 얹힌다.
        assertEquals(
            listOf(AttrValue.Int64(1), AttrValue.Int64(1)),
            eventsNamed("explore_list_load_more").map { it.attr("page_index") },
        )
    }

    // ---------- 서버 거절에서 스스로 회복 ----------

    @Test
    fun `정렬을 거절당하면 기본 정렬로 되돌려 다시 조회한다`() {
        loaded()
        repository.enqueue(throws(InvalidSortTypeException()), answer(challenges("x")))

        viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.COMPLETION_RATE))

        assertEquals(ExploreSort.POPULAR, state.sort)
        assertEquals(listOf("x"), itemIds())
        assertNull(state.errorMessage)
        assertEquals(ExploreSort.POPULAR, repository.calls.last().sort)
        // 요청한 정렬로 못 갔으므로 정렬 선호로 세지 않는다.
        assertFalse(names().contains("explore_sort_change"))
    }

    @Test
    fun `기본 정렬로도 거절당하면 그때는 오류를 보여준다`() {
        loaded()
        repository.alwaysAnswer(throws(InvalidSortTypeException()))

        viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.COMPLETION_RATE))

        assertEquals("정렬 조건을 다시 선택해 주세요.", state.errorMessage)
        assertFalse(state.isLoading)
        // 기본 정렬로 한 번만 더 시도한다 — 무한 재시도가 되면 안 된다.
        assertEquals(3, repository.calls.size)
    }

    @Test
    fun `필터를 거절당하면 필터를 비우고 다시 조회한다`() {
        loaded()
        repository.enqueue(throws(InvalidFilterValueException()), answer(challenges("x")))

        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.DETOX))))

        assertEquals(ExploreFilter.none, state.filter)
        assertEquals(listOf("x"), itemIds())
        assertNull(state.errorMessage)
    }

    @Test
    fun `빈 필터로도 거절당하면 그때는 오류를 보여준다`() {
        loaded()
        repository.alwaysAnswer(throws(InvalidFilterValueException()))

        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.DETOX))))

        assertEquals("필터 조건을 다시 선택해 주세요.", state.errorMessage)
        assertEquals(3, repository.calls.size)
    }

    @Test
    fun `첫 페이지에서 커서 오류가 나면 같은 조건으로 다시 묻는다`() {
        repository.enqueue(throws(CursorInvalidException()), answer(challenges("a")))

        viewModel.onIntent(ExploreListIntent.Load(category = "READING", sort = null))

        assertEquals(listOf("a"), itemIds())
        assertNull(state.errorMessage)
        assertEquals(
            List(2) { ExploreCall(ExploreFilter(categories = setOf(Category.READING)), ExploreSort.POPULAR, null) },
            repository.calls.toList(),
        )
    }

    @Test
    fun `메시지 없는 실패에도 화면에 보일 문구는 남는다`() {
        repository.enqueue(throws(RuntimeException()))

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertEquals("챌린지를 불러오지 못했어요", state.errorMessage)
        assertFalse(state.isLoading)
    }

    // ---------- 노출·클릭 ----------

    @Test
    fun `같은 카드 노출은 세션 안에서 한 번만 센다`() {
        // 중복을 세면 노출이 부풀어 상세 진입률(클릭/노출)이 실제보다 낮게 나온다.
        loaded()

        viewModel.onIntent(ExploreListIntent.CardImpression("b"))
        viewModel.onIntent(ExploreListIntent.CardImpression("b"))

        assertEquals(1, eventsNamed("challenge_card_impression").size)
    }

    @Test
    fun `노출 이벤트는 카드의 위치와 표본 유무를 함께 싣는다`() {
        repository.enqueue(
            answer(
                listOf(
                    exploreChallenge(challengeId = "a"),
                    exploreChallenge(
                        challengeId = "b",
                        startsSoon = true,
                        isFull = true,
                        eligible = false,
                        completionRate = null,
                        retentionRate = null,
                    ),
                ),
            ),
        )
        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        viewModel.onIntent(ExploreListIntent.CardImpression("b"))

        val impression = lastEvent("challenge_card_impression")
        assertEquals(AttrValue.Str("b"), impression.attr("challenge_id"))
        assertEquals(AttrValue.Int64(1), impression.attr("position"))
        assertEquals(AttrValue.Str("POPULAR"), impression.attr("sort"))
        assertEquals(AttrValue.Bool(true), impression.attr("is_full"))
        assertEquals(AttrValue.Bool(false), impression.attr("eligible"))
        // 시작 전이라 지표가 아예 없다 — 0% 로 그리는 카드와 구분돼야 클릭률 해석이 갈린다.
        assertEquals(AttrValue.Bool(false), impression.attr("has_metrics"))
    }

    @Test
    fun `목록에 없는 카드는 노출로 세지 않는다`() {
        loaded()

        viewModel.onIntent(ExploreListIntent.CardImpression("없는카드"))

        assertTrue(eventsNamed("challenge_card_impression").isEmpty())
    }

    @Test
    fun `목록이 갈리면 같은 카드도 다시 노출로 센다`() {
        // 필터·정렬이 바뀌면 목록 자체가 다른 화면이라 앞의 노출과 이어서 셀 수 없다.
        loaded()
        viewModel.onIntent(ExploreListIntent.CardImpression("b"))
        repository.enqueue(answer(challenges("a", "b", "c")))
        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.HOBBY))))

        viewModel.onIntent(ExploreListIntent.CardImpression("b"))

        assertEquals(2, eventsNamed("challenge_card_impression").size)
    }

    @Test
    fun `카드를 누르면 클릭을 남기고 상세로 넘긴다`() {
        loaded()

        viewModel.onIntent(ExploreListIntent.OpenChallenge("c"))

        val click = lastEvent("challenge_card_click")
        assertEquals(AttrValue.Str("c"), click.attr("challenge_id"))
        assertEquals(AttrValue.Int64(2), click.attr("position"))
        assertEquals(AttrValue.Str("list"), click.attr("source"))
        assertEquals(AttrValue.Str("POPULAR"), click.attr("sort"))
        // 노출 → 클릭 → 상세가 같은 challenge_id 로 이어져야 전환율이 계산된다.
        assertEquals(
            listOf(NavSignal.GoToDestPage(ChallengeDetailPage("c").toRoute())),
            navigation.signals.toList(),
        )
    }

    @Test
    fun `목록에서 못 찾은 카드를 눌러도 이동은 막지 않는다`() {
        loaded()

        viewModel.onIntent(ExploreListIntent.OpenChallenge("없는카드"))

        // 위치를 모른다고 상세 진입을 막으면 사용자가 손해다. 음수 위치만 0 으로 눌러 담는다.
        assertEquals(AttrValue.Int64(0), lastEvent("challenge_card_click").attr("position"))
        assertEquals(1, navigation.signals.size)
    }

    @Test
    fun `뒤로가기는 이동 신호만 보낸다`() {
        loaded()

        viewModel.onIntent(ExploreListIntent.Back)

        assertEquals(listOf(NavSignal.Back), navigation.signals.toList())
        assertTrue(names().isEmpty())
    }
}
