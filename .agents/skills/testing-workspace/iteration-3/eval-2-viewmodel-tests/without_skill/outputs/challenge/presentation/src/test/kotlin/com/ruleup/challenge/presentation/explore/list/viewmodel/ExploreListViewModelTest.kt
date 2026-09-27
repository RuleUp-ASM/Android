package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.observability.ChallengeCardSource
import com.ruleup.challenge.domain.observability.ChallengeEvents
import com.ruleup.challenge.domain.observability.ExploreListEntry
import com.ruleup.challenge.presentation.MainDispatcherRule
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.RecordingNavigationHelper
import com.ruleup.challenge.presentation.fake.RecordingSink
import com.ruleup.challenge.presentation.fake.recordingObservability
import com.ruleup.domain.entity.category.Category
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
 * 검증 축은 셋이다 — ① 인텐트가 **어떤 조건으로 조회를 부르는지**(필터·정렬·커서), ② 응답과 실패가
 * 상태로 어떻게 내려앉는지(서버 거절을 스스로 고쳐 재조회하는 경로 포함), ③ 퍼널 이벤트가 언제
 * 몇 번 나가는지. 이벤트는 이름·키 스키마를 `ChallengeEventsTest` 가 이미 고정하고 있으므로 여기서는
 * **팩토리에 넘긴 인자**가 맞는지만 본다(팩토리 출력끼리 비교).
 */
class ExploreListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val navigator = RecordingNavigationHelper()
    private val sink = RecordingSink()
    private var repository = FakeExploreRepository()

    // ---------- 진입 ----------

    @Test
    fun `라우트 카테고리는 초기 필터에 프리필된다`() {
        val viewModel = viewModel(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))

        val filter = ExploreFilter(categories = setOf(Category.EXERCISE))
        assertEquals(filter, viewModel.uiState.value.filter)
        assertEquals(filter, repository.calls.single().filter)
        assertEquals(
            ChallengeEvents.exploreListView(ExploreListEntry.CATEGORY, ExploreSort.default, filter),
            sink.business.first(),
        )
    }

    @Test
    fun `모르는 카테고리 값은 프리필하지 않는다`() {
        // 서버가 폐기 code 를 보내도 필터가 "아무것도 안 맞는 카테고리"로 잠기면 안 된다.
        val viewModel = viewModel(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.Load(category = "TIDYING", sort = null))

        assertEquals(ExploreFilter.none, viewModel.uiState.value.filter)
        assertEquals(
            ChallengeEvents.exploreListView(ExploreListEntry.ALL, ExploreSort.default, ExploreFilter.none),
            sink.business.first(),
        )
    }

    @Test
    fun `카테고리 인자가 없으면 전체 진입이다`() {
        val viewModel = viewModel(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertEquals(ExploreFilter.none, viewModel.uiState.value.filter)
        assertEquals(
            ChallengeEvents.exploreListView(ExploreListEntry.ALL, ExploreSort.default, ExploreFilter.none),
            sink.business.first(),
        )
    }

    @Test
    fun `라우트 정렬을 초기 정렬로 쓴다`() {
        val viewModel = viewModel(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = "RECENT"))

        assertEquals(ExploreSort.RECENT, viewModel.uiState.value.sort)
        assertEquals(ExploreSort.RECENT, repository.calls.single().sort)
    }

    @Test
    fun `모르는 정렬 값은 기본 정렬로 떨어진다`() {
        val viewModel = viewModel(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = "MOST_FUN"))

        assertEquals(ExploreSort.default, viewModel.uiState.value.sort)
        assertEquals(ExploreSort.default, repository.calls.single().sort)
    }

    @Test
    fun `진입 인텐트가 두 번 와도 한 번만 조회한다`() {
        // 화면 재구성으로 Load 가 다시 올라와도 목록을 다시 받거나 진입 이벤트를 부풀리지 않는다.
        val viewModel = viewModel(page(challenge("c1")), page(challenge("c2")))

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertEquals(1, repository.calls.size)
        assertEquals(listOf("explore_list_view"), sink.names)
    }

    // ---------- 첫 페이지 ----------

    @Test
    fun `첫 페이지는 목록과 다음 커서를 채우고 로딩을 끝낸다`() {
        val viewModel = loaded(page(challenge("c1"), challenge("c2"), nextCursor = "cur-1"))

        val state = viewModel.uiState.value
        assertEquals(listOf("c1", "c2"), state.items.map { it.challengeId })
        assertEquals("cur-1", state.nextCursor)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `조회가 끝나기 전까지는 로딩 상태다`() {
        val viewModel = viewModel(page(challenge("c1")))
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.items.isEmpty())

        gate.complete(Unit)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.items.size)
    }

    @Test
    fun `빈 결과는 진입 이벤트와 별개로 남긴다`() {
        // 필터 사용률과 빈 결과율은 분모가 달라 하나로 합칠 수 없다 — 중복 전송이 정상이다.
        val viewModel = loaded(page())

        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals(listOf("explore_list_view", "explore_empty_result"), sink.names)
        assertEquals(
            ChallengeEvents.exploreEmptyResult(ExploreFilter.none, ExploreSort.default),
            sink.business.last(),
        )
    }

    @Test
    fun `결과가 있으면 빈 결과 이벤트를 남기지 않는다`() {
        loaded(page(challenge("c1")))

        assertEquals(listOf("explore_list_view"), sink.names)
    }

    @Test
    fun `조회 실패는 로딩을 끝내고 메시지를 남긴다`() {
        val viewModel = loaded(Result.failure(IllegalStateException("서버가 응답하지 않았어요")))

        val state = viewModel.uiState.value
        assertEquals("서버가 응답하지 않았어요", state.errorMessage)
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `메시지 없는 실패도 화면 문구를 채운다`() {
        val viewModel = loaded(Result.failure(RuntimeException()))

        assertEquals("챌린지를 불러오지 못했어요", viewModel.uiState.value.errorMessage)
    }

    // ---------- 필터 · 정렬 ----------

    @Test
    fun `필터 적용은 첫 페이지부터 다시 받고 결과 수를 실어 남긴다`() {
        val viewModel =
            loaded(
                page(challenge("c1"), nextCursor = "cur-1"),
                page(challenge("c9"), challenge("c8")),
            )
        val filter = ExploreFilter(categories = setOf(Category.STUDY), verifyType = VerificationType.AUTO)

        viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))

        val state = viewModel.uiState.value
        assertEquals(filter, state.filter)
        assertEquals(listOf("c9", "c8"), state.items.map { it.challengeId })
        // 이전 페이지의 커서를 물고 가면 필터가 바뀐 목록의 중간부터 받게 된다.
        assertEquals(listOf(null, null), repository.calls.map { it.cursor })
        assertEquals(filter, repository.calls.last().filter)
        // result_count 를 실어야 해서 인텐트 시점이 아니라 응답 시점에 나간다.
        assertEquals(ChallengeEvents.exploreFilterApply(filter, 2), sink.business.last())
    }

    @Test
    fun `정렬 변경은 이전 정렬과 함께 남긴다`() {
        val viewModel = loaded(page(challenge("c1")), page(challenge("c2")))

        viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.DEADLINE))

        assertEquals(ExploreSort.DEADLINE, viewModel.uiState.value.sort)
        assertEquals(ExploreSort.DEADLINE, repository.calls.last().sort)
        assertEquals(
            ChallengeEvents.exploreSortChange(from = ExploreSort.POPULAR, to = ExploreSort.DEADLINE, resultCount = 1),
            sink.business.last(),
        )
    }

    @Test
    fun `티어 조건 끄기는 나머지 필터를 유지한다`() {
        val filter =
            ExploreFilter(
                categories = setOf(Category.EXERCISE),
                verifyType = VerificationType.AUTO,
                eligibleOnly = true,
            )
        val viewModel = loaded(page(challenge("c1")), page(), page(challenge("c2")))
        viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))

        viewModel.onIntent(ExploreListIntent.ClearEligibleOnly)

        assertEquals(filter.copy(eligibleOnly = false), viewModel.uiState.value.filter)
        assertEquals(filter.copy(eligibleOnly = false), repository.calls.last().filter)
        // 완화는 사용자가 필터를 새로 고른 게 아니다 — filter_apply 를 다시 세면 필터 사용률이 부풀어 오른다.
        assertEquals(
            listOf("explore_list_view", "explore_filter_apply", "explore_empty_result"),
            sink.names,
        )
    }

    // ---------- 서버 거절 복구 ----------

    @Test
    fun `정렬 오류는 기본 정렬로 되돌려 다시 받는다`() {
        val viewModel =
            loaded(
                Result.failure(InvalidSortTypeException()),
                page(challenge("c1")),
                sort = "RECENT",
            )

        assertEquals(listOf(ExploreSort.RECENT, ExploreSort.default), repository.calls.map { it.sort })
        assertEquals(ExploreSort.default, viewModel.uiState.value.sort)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `기본 정렬에서 난 정렬 오류는 되돌릴 곳이 없어 메시지를 낸다`() {
        val viewModel = loaded(Result.failure(InvalidSortTypeException()))

        assertEquals(1, repository.calls.size)
        assertEquals(InvalidSortTypeException().message, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `필터 오류는 필터를 비우고 다시 받는다`() {
        val viewModel =
            loaded(
                Result.failure(InvalidFilterValueException()),
                page(challenge("c1")),
                category = "EXERCISE",
            )

        assertEquals(
            listOf(ExploreFilter(categories = setOf(Category.EXERCISE)), ExploreFilter.none),
            repository.calls.map { it.filter },
        )
        assertEquals(ExploreFilter.none, viewModel.uiState.value.filter)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `빈 필터에서 난 필터 오류는 비울 곳이 없어 메시지를 낸다`() {
        val viewModel = loaded(Result.failure(InvalidFilterValueException()))

        assertEquals(1, repository.calls.size)
        assertEquals(InvalidFilterValueException().message, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `첫 페이지 커서 오류는 같은 조건으로 조용히 다시 받는다`() {
        val viewModel =
            loaded(
                Result.failure(CursorInvalidException()),
                page(challenge("c1")),
            )

        assertEquals(listOf(null, null), repository.calls.map { it.cursor })
        assertEquals(1, viewModel.uiState.value.items.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ---------- 다음 페이지 ----------

    @Test
    fun `다음 페이지는 커서를 실어 목록에 이어 붙인다`() {
        val viewModel =
            loaded(
                page(challenge("c1"), nextCursor = "cur-1"),
                page(challenge("c2")),
            )

        viewModel.onIntent(ExploreListIntent.LoadMore)

        val state = viewModel.uiState.value
        assertEquals(listOf("c1", "c2"), state.items.map { it.challengeId })
        assertNull(state.nextCursor)
        assertFalse(state.isLoadingMore)
        assertEquals(listOf(null, "cur-1"), repository.calls.map { it.cursor })
        assertEquals(ChallengeEvents.exploreListLoadMore(1, ExploreSort.default), sink.business.last())
    }

    @Test
    fun `마지막 페이지에서는 다음 페이지를 부르지 않는다`() {
        val viewModel = loaded(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(1, repository.calls.size)
    }

    @Test
    fun `진행 중이면 다음 페이지를 겹쳐 부르지 않는다`() {
        // 하단 근접 판정은 스크롤마다 올라온다 — 겹쳐 부르면 같은 페이지가 두 번 붙는다.
        val viewModel = loaded(page(challenge("c1"), nextCursor = "cur-1"), page(challenge("c2")))
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate

        viewModel.onIntent(ExploreListIntent.LoadMore)
        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertTrue(viewModel.uiState.value.isLoadingMore)
        assertEquals(2, repository.calls.size)

        gate.complete(Unit)

        assertEquals(listOf("c1", "c2"), viewModel.uiState.value.items.map { it.challengeId })
    }

    @Test
    fun `다음 페이지 실패는 기존 목록을 지우지 않는다`() {
        val viewModel =
            loaded(
                page(challenge("c1"), nextCursor = "cur-1"),
                Result.failure(IllegalStateException("네트워크가 끊겼어요")),
            )

        viewModel.onIntent(ExploreListIntent.LoadMore)

        val state = viewModel.uiState.value
        assertEquals(listOf("c1"), state.items.map { it.challengeId })
        assertTrue(state.loadMoreFailed)
        assertFalse(state.isLoadingMore)
        // 하단에서만 재시도한다 — 전체 에러 화면으로 덮지 않는다.
        assertNull(state.errorMessage)
        assertEquals("cur-1", state.nextCursor)
    }

    @Test
    fun `다음 페이지 커서 오류는 첫 페이지부터 다시 받는다`() {
        val viewModel =
            loaded(
                page(challenge("c1"), nextCursor = "cur-1"),
                Result.failure(CursorInvalidException()),
                page(challenge("c9")),
            )

        viewModel.onIntent(ExploreListIntent.LoadMore)

        val state = viewModel.uiState.value
        assertEquals(listOf("c9"), state.items.map { it.challengeId })
        assertFalse(state.loadMoreFailed)
        assertNull(state.errorMessage)
        assertEquals(listOf(null, "cur-1", null), repository.calls.map { it.cursor })
    }

    @Test
    fun `페이지 깊이는 첫 페이지를 다시 받을 때 초기화된다`() {
        val viewModel =
            loaded(
                page(challenge("c1"), nextCursor = "cur-1"),
                page(challenge("c2"), nextCursor = "cur-2"),
                page(challenge("c3")),
                page(challenge("c4"), nextCursor = "cur-9"),
                page(challenge("c5")),
            )

        viewModel.onIntent(ExploreListIntent.LoadMore)
        viewModel.onIntent(ExploreListIntent.LoadMore)
        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.READING))))
        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(
            listOf(
                ChallengeEvents.exploreListLoadMore(1, ExploreSort.default),
                ChallengeEvents.exploreListLoadMore(2, ExploreSort.default),
                ChallengeEvents.exploreListLoadMore(1, ExploreSort.default),
            ),
            sink.business.filter { it.name == "explore_list_load_more" },
        )
    }

    // ---------- 노출 · 클릭 ----------

    @Test
    fun `카드 노출은 위치와 카드 속성을 실어 남긴다`() {
        val viewModel =
            loaded(
                page(
                    challenge("c1"),
                    challenge("c2", isFull = true, eligible = false, completionRate = null, retentionRate = null),
                ),
            )

        viewModel.onIntent(ExploreListIntent.CardImpression("c2"))

        assertEquals(
            ChallengeEvents.challengeCardImpression(
                challengeId = "c2",
                position = 1,
                sort = ExploreSort.default,
                isFull = true,
                eligible = false,
                hasMetrics = false,
            ),
            sink.business.last(),
        )
    }

    @Test
    fun `같은 카드가 다시 노출돼도 한 번만 센다`() {
        // 스크롤로 같은 카드가 여러 번 들어오면 노출 수가 부풀어 상세 진입률이 낮게 나온다.
        val viewModel = loaded(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.CardImpression("c1"))
        viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

        assertEquals(1, sink.names.count { it == "challenge_card_impression" })
    }

    @Test
    fun `목록에 없는 카드는 노출로 세지 않는다`() {
        val viewModel = loaded(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.CardImpression("없는-카드"))

        assertEquals(0, sink.names.count { it == "challenge_card_impression" })
    }

    @Test
    fun `목록이 갈리면 같은 카드를 다시 노출로 센다`() {
        // 필터가 바뀌면 목록 자체가 달라진다 — 중복 제거를 유지하면 새 목록의 노출이 통째로 사라진다.
        val viewModel = loaded(page(challenge("c1")), page(challenge("c1")))
        viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.MIND))))
        viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

        assertEquals(2, sink.names.count { it == "challenge_card_impression" })
    }

    @Test
    fun `카드 클릭은 같은 challenge_id 로 상세를 연다`() {
        val viewModel = loaded(page(challenge("c1"), challenge("c2")))

        viewModel.onIntent(ExploreListIntent.OpenChallenge("c2"))

        assertEquals(listOf(ChallengeDetailPage("c2").toRoute()), navigator.routes)
        assertEquals(
            ChallengeEvents.challengeCardClick(
                challengeId = "c2",
                position = 1,
                source = ChallengeCardSource.LIST,
                sort = ExploreSort.default,
            ),
            sink.business.last(),
        )
    }

    @Test
    fun `뒤로 가기는 조회를 만들지 않는다`() {
        val viewModel = loaded(page(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.Back)

        assertEquals(1, navigator.backCount)
        assertTrue(navigator.routes.isEmpty())
        assertEquals(1, repository.calls.size)
    }

    // ---------- 조립 ----------

    /** [outcomes] 는 explore() 호출 순서대로 소비된다 — 동나면 빈 페이지가 온다. */
    private fun viewModel(vararg outcomes: Result<ExploreResult>): ExploreListViewModel {
        repository = FakeExploreRepository(outcomes.toList())
        return ExploreListViewModel(repository, navigator, recordingObservability(sink))
    }

    /** 진입까지 끝난 ViewModel. 첫 페이지는 [outcomes] 의 첫 응답으로 채워진다. */
    private fun loaded(
        vararg outcomes: Result<ExploreResult>,
        category: String? = null,
        sort: String? = null,
    ): ExploreListViewModel =
        viewModel(*outcomes).also { it.onIntent(ExploreListIntent.Load(category = category, sort = sort)) }

    /** 성공 응답 하나. 실패는 호출부에서 `Result.failure(...)` 로 섞는다. */
    private fun page(
        vararg items: ExploreChallenge,
        nextCursor: String? = null,
    ) = Result.success(ExploreResult(items = items.toList(), nextCursor = nextCursor, hasNext = nextCursor != null))

    private fun challenge(
        id: String,
        isFull: Boolean = false,
        eligible: Boolean = true,
        startsSoon: Boolean = false,
        completionRate: Double? = 0.82,
        retentionRate: Double? = 0.71,
    ) = ExploreChallenge(
        challengeId = id,
        title = "매일 아침 6시 기상",
        imageUrl = null,
        category = Category.WAKE_SLEEP,
        verificationType = VerificationType.AUTO,
        startsSoon = startsSoon,
        participantCount = 12,
        capacity = 20,
        isFull = isFull,
        minTier = null,
        eligible = eligible,
        completionRate = completionRate,
        retentionRate = retentionRate,
        dday = 7,
        startDate = "2026-08-01",
        endDate = "2026-09-07",
        createdAt = "2026-07-30T09:00:00+09:00",
    )
}
