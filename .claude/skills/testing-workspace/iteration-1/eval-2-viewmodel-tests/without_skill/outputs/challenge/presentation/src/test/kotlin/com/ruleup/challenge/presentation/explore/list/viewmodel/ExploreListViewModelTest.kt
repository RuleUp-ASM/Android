package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.RecordingNavigationHelper
import com.ruleup.challenge.presentation.testing.MainDispatcherRule
import com.ruleup.domain.entity.category.Category
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
 * 이 화면의 계약은 세 갈래다. ① (필터, 정렬) 이 바뀌면 **첫 페이지부터** 다시 받고 이어 붙이지 않는다,
 * ② 서버가 조건을 거절하면 사용자에게 되묻지 않고 스스로 고쳐 재조회한다, ③ 탐색 퍼널 이벤트는
 * 릴리즈 게이트라 하나라도 빠지거나 중복되면 전환율이 계산되지 않는다. 셋 다 화면을 띄우지 않고도
 * 관측되므로 여기서 검증한다.
 *
 * 레포지토리에 **무엇을 보냈는지**(커서·필터·정렬)까지 단언하는 이유는, 반환값만 맞아도 커서를 빼먹거나
 * 필터를 흘리면 목록이 조용히 잘못 채워지기 때문이다.
 */
class ExploreListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExploreRepository()
    private val navigation = RecordingNavigationHelper()
    private val sink = RecordingSink()

    private fun viewModel() =
        ExploreListViewModel(
            exploreRepository = repository,
            navigationHelper = navigation,
            observability = testObservability(sink = sink),
        )

    /** 첫 페이지까지 받아 둔 화면. 진입 자체가 관심사가 아닌 테스트의 출발점이다. */
    private fun loadedViewModel(
        items: List<ExploreChallenge> = listOf(challenge("c1")),
        nextCursor: String? = null,
    ): ExploreListViewModel {
        repository.succeed(items = items, nextCursor = nextCursor)
        return viewModel().also { it.onIntent(ExploreListIntent.Load(category = null, sort = null)) }
    }

    // ---------- 진입 ----------

    @Test
    fun `카테고리 타일로 들어오면 그 카테고리를 필터에 채우고 카테고리 진입으로 남긴다`() {
        repository.succeed(items = listOf(challenge("c1")))

        viewModel().onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))

        assertEquals(setOf(Category.EXERCISE), repository.calls.single().filter.categories)
        val view = sink.named("explore_list_view").single()
        assertEquals("category", view.str("entry"))
        assertEquals("categories=EXERCISE", view.str("filters"))
    }

    @Test
    fun `모르는 카테고리 값은 필터를 비우고 전체 진입으로 남긴다`() {
        // 서버가 폐기 code 를 보내도 목록은 전체로 열려야 한다. 빈 필터로 조회하지 않으면 화면이 통째로 빈다.
        repository.succeed(items = listOf(challenge("c1")))

        viewModel().onIntent(ExploreListIntent.Load(category = "TIDYING", sort = null))

        assertEquals(ExploreFilter.none, repository.calls.single().filter)
        assertEquals("all", sink.named("explore_list_view").single().str("entry"))
        assertEquals("none", sink.named("explore_list_view").single().str("filters"))
    }

    @Test
    fun `라우트 정렬 인자를 그대로 쓴다`() {
        repository.succeed()

        viewModel().onIntent(ExploreListIntent.Load(category = null, sort = "RECENT"))

        assertEquals(ExploreSort.RECENT, repository.calls.single().sort)
        assertEquals("RECENT", sink.named("explore_list_view").single().str("sort"))
    }

    @Test
    fun `모르는 정렬 인자는 기본 정렬로 떨어진다`() {
        repository.succeed()

        val viewModel = viewModel()
        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = "BY_VIBES"))

        assertEquals(ExploreSort.POPULAR, repository.calls.single().sort)
        assertEquals(ExploreSort.POPULAR, viewModel.uiState.value.sort)
    }

    @Test
    fun `화면 진입 조회는 한 번만 한다`() {
        // 화면이 재구성될 때마다 Load 가 다시 올라온다. 막지 않으면 진입 이벤트가 부풀어 분모가 망가진다.
        repository.succeed(items = listOf(challenge("c1")))
        val viewModel = viewModel()

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))
        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertEquals(1, repository.calls.size)
        assertEquals(1, sink.named("explore_list_view").size)
    }

    @Test
    fun `첫 페이지 결과가 상태에 들어간다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1"), challenge("c2")), nextCursor = "cur1")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("c1", "c2"), state.items.map { it.challengeId })
        assertEquals("cur1", state.nextCursor)
        assertNull(state.errorMessage)
    }

    // ---------- 빈 결과 ----------

    @Test
    fun `결과가 0건이면 빈 결과를 따로 남긴다`() {
        loadedViewModel(items = emptyList())

        assertEquals(1, sink.named("explore_empty_result").size)
    }

    @Test
    fun `결과가 있으면 빈 결과를 남기지 않는다`() {
        loadedViewModel(items = listOf(challenge("c1")))

        assertTrue(sink.named("explore_empty_result").isEmpty())
    }

    @Test
    fun `티어 조건 때문에 0건이면 조건 완화를 안내한다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1")))
        repository.succeed(items = emptyList())

        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(eligibleOnly = true)))

        assertEquals(EmptyReason.TIER_FILTER, viewModel.uiState.value.emptyReason)
    }

    @Test
    fun `지표 정렬에서 0건이면 조건이 아니라 기록 부족으로 안내한다`() {
        // 표본 미달 방이 목록에서 아예 빠지는 정렬이라 "조건이 좁다"는 오안내가 된다.
        val viewModel = loadedViewModel(items = listOf(challenge("c1")))
        repository.succeed(items = emptyList())

        viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.COMPLETION_RATE))

        assertEquals(EmptyReason.LOW_SAMPLE, viewModel.uiState.value.emptyReason)
    }

    // ---------- 필터 · 정렬 ----------

    @Test
    fun `필터를 적용하면 커서를 버리고 첫 페이지부터 다시 받는다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1"), challenge("c2")), nextCursor = "cur1")
        repository.succeed(items = listOf(challenge("c3")), nextCursor = "cur2")

        val filter = ExploreFilter(categories = setOf(Category.STUDY), eligibleOnly = true)
        viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))

        val call = repository.calls.last()
        assertEquals(filter, call.filter)
        assertNull(call.cursor)
        // 이어 붙이지 않는다 — 붙이면 필터에 안 맞는 카드가 목록에 남는다.
        assertEquals(listOf("c3"), viewModel.uiState.value.items.map { it.challengeId })
        assertEquals("cur2", viewModel.uiState.value.nextCursor)
    }

    @Test
    fun `필터 적용은 결과 수와 함께 남긴다`() {
        // result_count 는 응답이 와야 알 수 있어 인텐트 시점이 아니라 응답 시점에 나가야 한다.
        val viewModel = loadedViewModel()
        repository.succeed(items = listOf(challenge("c3"), challenge("c4")))

        viewModel.onIntent(
            ExploreListIntent.ApplyFilter(
                ExploreFilter(
                    categories = setOf(Category.STUDY),
                    verifyType = VerificationType.MANUAL,
                    eligibleOnly = true,
                ),
            ),
        )

        val event = sink.named("explore_filter_apply").single()
        assertEquals("STUDY", event.str("categories"))
        assertEquals("MANUAL", event.str("verify_type"))
        assertTrue(event.bool("eligible_only"))
        assertEquals(2L, event.int("result_count"))
    }

    @Test
    fun `정렬을 바꾸면 어디서 어디로 옮겼는지 남긴다`() {
        val viewModel = loadedViewModel()
        repository.succeed(items = listOf(challenge("c3")))

        viewModel.onIntent(ExploreListIntent.SelectSort(ExploreSort.RECENT))

        assertEquals(ExploreSort.RECENT, repository.calls.last().sort)
        assertEquals(ExploreSort.RECENT, viewModel.uiState.value.sort)
        val event = sink.named("explore_sort_change").single()
        assertEquals("POPULAR", event.str("sort_from"))
        assertEquals("RECENT", event.str("sort_to"))
        assertEquals(1L, event.int("result_count"))
    }

    @Test
    fun `티어 조건만 끄고 나머지 필터는 유지한다`() {
        // 빈 결과에서 제안하는 완화라, 사용자가 고른 카테고리까지 날리면 다른 화면이 돼 버린다.
        val viewModel = loadedViewModel()
        repository.succeed()
        val filter =
            ExploreFilter(
                categories = setOf(Category.STUDY),
                verifyType = VerificationType.MANUAL,
                eligibleOnly = true,
            )
        viewModel.onIntent(ExploreListIntent.ApplyFilter(filter))
        repository.succeed()

        viewModel.onIntent(ExploreListIntent.ClearEligibleOnly)

        assertEquals(filter.copy(eligibleOnly = false), repository.calls.last().filter)
    }

    // ---------- 다음 페이지 ----------

    @Test
    fun `다음 페이지는 커서로 이어 붙인다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1")), nextCursor = "cur1")
        repository.succeed(items = listOf(challenge("c2")), nextCursor = null)

        viewModel.onIntent(ExploreListIntent.LoadMore)

        val call = repository.calls.last()
        assertEquals("cur1", call.cursor)
        assertEquals(ExploreFilter.none, call.filter)
        assertEquals(ExploreSort.POPULAR, call.sort)
        val state = viewModel.uiState.value
        assertEquals(listOf("c1", "c2"), state.items.map { it.challengeId })
        assertNull(state.nextCursor)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `다음 페이지를 받으면 스크롤 깊이를 남긴다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1")), nextCursor = "cur1")
        repository.succeed(items = listOf(challenge("c2")), nextCursor = "cur2")
        repository.succeed(items = listOf(challenge("c3")), nextCursor = null)

        viewModel.onIntent(ExploreListIntent.LoadMore)
        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(listOf(1L, 2L), sink.named("explore_list_load_more").map { it.int("page_index") })
    }

    @Test
    fun `마지막 페이지에서는 다음 페이지를 부르지 않는다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1")), nextCursor = null)

        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(1, repository.calls.size)
    }

    @Test
    fun `요청이 떠 있는 동안 다음 페이지를 중복 호출하지 않는다`() {
        // 하단 근접 판정은 스크롤마다 올라온다. 막지 않으면 같은 커서로 같은 페이지를 두 번 붙인다.
        val viewModel = loadedViewModel(items = listOf(challenge("c1")), nextCursor = "cur1")
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        repository.succeed(items = listOf(challenge("c2")), nextCursor = null)

        viewModel.onIntent(ExploreListIntent.LoadMore)
        assertTrue(viewModel.uiState.value.isLoadingMore)
        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(2, repository.calls.size)
        gate.complete(Unit)
        assertEquals(listOf("c1", "c2"), viewModel.uiState.value.items.map { it.challengeId })
    }

    @Test
    fun `다음 페이지 실패는 목록을 지우지 않고 하단 재시도로 남는다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1")), nextCursor = "cur1")
        repository.fail(IllegalStateException("네트워크"))

        viewModel.onIntent(ExploreListIntent.LoadMore)

        val state = viewModel.uiState.value
        assertTrue(state.loadMoreFailed)
        assertFalse(state.isLoadingMore)
        assertEquals(listOf("c1"), state.items.map { it.challengeId })
        // 전면 에러로 올리면 이미 보고 있던 목록이 통째로 사라진다.
        assertNull(state.errorMessage)
        assertEquals("cur1", state.nextCursor)
    }

    @Test
    fun `상한 커서는 사용자에게 알리지 않고 첫 페이지부터 다시 받는다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1")), nextCursor = "cur1")
        repository.fail(CursorInvalidException())
        repository.succeed(items = listOf(challenge("c9")), nextCursor = "cur9")

        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertNull(repository.calls.last().cursor)
        val state = viewModel.uiState.value
        assertEquals(listOf("c9"), state.items.map { it.challengeId })
        assertFalse(state.loadMoreFailed)
        assertNull(state.errorMessage)
    }

    @Test
    fun `커서 복구로 첫 페이지를 다시 받으면 스크롤 깊이도 처음부터 센다`() {
        // 깊이는 "이번 목록을 얼마나 내려갔나"라, 리셋하지 않으면 재조회가 깊이를 부풀린다.
        val viewModel = loadedViewModel(items = listOf(challenge("c1")), nextCursor = "cur1")
        repository.fail(CursorInvalidException())
        repository.succeed(items = listOf(challenge("c9")), nextCursor = "cur9")
        viewModel.onIntent(ExploreListIntent.LoadMore)
        repository.succeed(items = listOf(challenge("c10")), nextCursor = null)

        viewModel.onIntent(ExploreListIntent.LoadMore)

        assertEquals(listOf(1L), sink.named("explore_list_load_more").map { it.int("page_index") })
    }

    // ---------- 서버 거절 복구 ----------

    @Test
    fun `정렬 오류는 기본 정렬로 되돌려 다시 받는다`() {
        repository.fail(InvalidSortTypeException())
        repository.succeed(items = listOf(challenge("c1")))
        val viewModel = viewModel()

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = "DEADLINE"))

        assertEquals(listOf(ExploreSort.DEADLINE, ExploreSort.POPULAR), repository.calls.map { it.sort })
        val state = viewModel.uiState.value
        assertEquals(ExploreSort.POPULAR, state.sort)
        assertEquals(listOf("c1"), state.items.map { it.challengeId })
        assertNull(state.errorMessage)
    }

    @Test
    fun `기본 정렬에서도 정렬 오류면 되돌릴 곳이 없어 에러로 남는다`() {
        // 되돌릴 곳이 없는데 재시도하면 같은 요청을 무한히 반복한다.
        repository.fail(InvalidSortTypeException())
        val viewModel = viewModel()

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertEquals(1, repository.calls.size)
        assertEquals(InvalidSortTypeException().message, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `필터 오류는 필터를 비우고 다시 받는다`() {
        repository.fail(InvalidFilterValueException())
        repository.succeed(items = listOf(challenge("c1")))
        val viewModel = viewModel()

        viewModel.onIntent(ExploreListIntent.Load(category = "EXERCISE", sort = null))

        assertEquals(ExploreFilter.none, repository.calls.last().filter)
        assertEquals(ExploreFilter.none, viewModel.uiState.value.filter)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `필터가 이미 비어 있는데 필터 오류면 에러로 남는다`() {
        repository.fail(InvalidFilterValueException())
        val viewModel = viewModel()

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertEquals(1, repository.calls.size)
        assertEquals(InvalidFilterValueException().message, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `그 밖의 오류는 서버 메시지를 그대로 보여준다`() {
        repository.fail(IllegalStateException("잠시 후 다시 시도해 주세요"))
        val viewModel = viewModel()

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        val state = viewModel.uiState.value
        assertEquals("잠시 후 다시 시도해 주세요", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `메시지 없는 오류에도 빈 화면을 두지 않는다`() {
        repository.fail(IllegalStateException())
        val viewModel = viewModel()

        viewModel.onIntent(ExploreListIntent.Load(category = null, sort = null))

        assertEquals("챌린지를 불러오지 못했어요", viewModel.uiState.value.errorMessage)
    }

    // ---------- 노출 ----------

    @Test
    fun `카드 노출은 위치와 카드 속성을 함께 남긴다`() {
        val viewModel =
            loadedViewModel(
                items =
                    listOf(
                        challenge("c1"),
                        challenge("c2", isFull = true, eligible = false, completionRate = null),
                    ),
            )

        viewModel.onIntent(ExploreListIntent.CardImpression("c2"))

        val event = sink.named("challenge_card_impression").single()
        assertEquals("c2", event.str("challenge_id"))
        assertEquals(1L, event.int("position"))
        assertEquals("POPULAR", event.str("sort"))
        assertTrue(event.bool("is_full"))
        assertFalse(event.bool("eligible"))
        assertFalse(event.bool("has_metrics"))
    }

    @Test
    fun `같은 카드가 다시 보여도 노출은 한 번만 남긴다`() {
        // 스크롤로 재진입할 때마다 세면 노출이 부풀어 상세 진입률이 실제보다 낮게 나온다.
        val viewModel = loadedViewModel(items = listOf(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.CardImpression("c1"))
        viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

        assertEquals(1, sink.named("challenge_card_impression").size)
    }

    @Test
    fun `목록에 없는 카드 노출은 남기지 않는다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.CardImpression("없는카드"))

        assertTrue(sink.named("challenge_card_impression").isEmpty())
    }

    @Test
    fun `필터가 바뀌면 노출 중복 제거를 다시 시작한다`() {
        // 목록 자체가 다른 화면이라 같은 카드라도 새 노출이다.
        val viewModel = loadedViewModel(items = listOf(challenge("c1")))
        viewModel.onIntent(ExploreListIntent.CardImpression("c1"))
        repository.succeed(items = listOf(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.ApplyFilter(ExploreFilter(categories = setOf(Category.STUDY))))
        viewModel.onIntent(ExploreListIntent.CardImpression("c1"))

        assertEquals(2, sink.named("challenge_card_impression").size)
    }

    // ---------- 이동 ----------

    @Test
    fun `카드를 누르면 클릭을 남기고 상세로 보낸다`() {
        val viewModel = loadedViewModel(items = listOf(challenge("c1"), challenge("c2")))

        viewModel.onIntent(ExploreListIntent.OpenChallenge("c2"))

        val event = sink.named("challenge_card_click").single()
        assertEquals("c2", event.str("challenge_id"))
        assertEquals(1L, event.int("position"))
        assertEquals("list", event.str("source"))
        assertEquals("POPULAR", event.str("sort"))
        // 노출 → 클릭 → 상세가 같은 challenge_id 로 이어져야 전환율이 계산된다.
        assertEquals(
            ChallengeDetailPage("c2").toRoute(),
            navigation.routes.single(),
        )
    }

    @Test
    fun `목록에서 못 찾은 카드도 위치를 음수로 남기지 않는다`() {
        // indexOfFirst 의 -1 이 그대로 나가면 집계에서 위치 분포가 깨진다.
        val viewModel = loadedViewModel(items = listOf(challenge("c1")))

        viewModel.onIntent(ExploreListIntent.OpenChallenge("c9"))

        assertEquals(0L, sink.named("challenge_card_click").single().int("position"))
        assertEquals(1, navigation.routes.size)
    }

    @Test
    fun `뒤로가기는 네비게이션에 위임한다`() {
        val viewModel = loadedViewModel()

        viewModel.onIntent(ExploreListIntent.Back)

        assertEquals(1, navigation.backCount)
        assertEquals(1, repository.calls.size)
    }
}

private fun challenge(
    id: String,
    isFull: Boolean = false,
    eligible: Boolean = true,
    completionRate: Double? = 0.8,
) = ExploreChallenge(
    challengeId = id,
    title = id,
    imageUrl = null,
    category = Category.EXERCISE,
    verificationType = VerificationType.AUTO,
    startsSoon = false,
    participantCount = 3,
    capacity = 10,
    isFull = isFull,
    minTier = null,
    eligible = eligible,
    completionRate = completionRate,
    retentionRate = null,
    dday = 5,
    startDate = null,
    endDate = null,
    createdAt = null,
)

private fun RecordingSink.named(name: String): List<BusinessPayload.Custom> =
    payloads.filterIsInstance<BusinessPayload.Custom>().filter { it.name == name }

private fun BusinessPayload.Custom.str(key: String) = (attrs.entries.getValue(AttrKey(key)) as AttrValue.Str).v

private fun BusinessPayload.Custom.int(key: String) = (attrs.entries.getValue(AttrKey(key)) as AttrValue.Int64).v

private fun BusinessPayload.Custom.bool(key: String) = (attrs.entries.getValue(AttrKey(key)) as AttrValue.Bool).v
