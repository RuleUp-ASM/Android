package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.presentation.fake.exploreChallenge
import com.ruleup.domain.entity.category.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 상태에서 파생되는 두 규칙 — **왜 비었는가**와 **더 불러올 수 있는가** — 를 여기서 끝낸다.
 *
 * ViewModel 테스트가 이 조합을 다시 훑지 않게 하려고 아래층으로 내렸다. 빈 사유는 사유마다 문구와
 * CTA 가 달라서, 잘못 고르면 사용자는 "필터를 푸세요"를 보고 필터를 풀었는데도 여전히 0건인 화면을
 * 마주한다 — 정작 원인은 기록이 모자란 방뿐이었던 경우다.
 */
class ExploreListStateTest {
    @Test
    fun `지표 정렬로 0건이면 조건이 좁은 게 아니라 기록이 없는 것이다`() {
        // COMPLETION_RATE·SUCCESS_FAIL_RATIO 는 표본 미달 방을 목록에서 아예 뺀다.
        // 필터가 함께 걸려 있어도 이 사유가 먼저다 — 필터를 풀어도 결과가 늘지 않기 때문이다.
        val state = state(sort = ExploreSort.COMPLETION_RATE, filter = ExploreFilter(eligibleOnly = true))

        assertEquals(EmptyReason.LOW_SAMPLE, state.emptyReason)
    }

    @Test
    fun `티어 조건이 켜진 채 0건이면 그것부터 풀라고 안내한다`() {
        // 티어 컷은 초기 풀을 가장 크게 깎는다. 다른 필터보다 먼저 완화를 제안한다.
        val state =
            state(filter = ExploreFilter(categories = setOf(Category.EXERCISE), eligibleOnly = true))

        assertEquals(EmptyReason.TIER_FILTER, state.emptyReason)
    }

    @Test
    fun `티어 조건 없이 필터만 걸려 0건이면 필터 탓이라고 안내한다`() {
        val state = state(filter = ExploreFilter(categories = setOf(Category.EXERCISE)))

        assertEquals(EmptyReason.FILTERED, state.emptyReason)
    }

    @Test
    fun `아무 조건 없이 0건이면 이 분류에 방이 없는 것이다`() {
        assertEquals(EmptyReason.CATEGORY_EMPTY, state().emptyReason)
    }

    @Test
    fun `불러오는 중이거나 보여줄 게 있으면 빈 안내를 하지 않는다`() {
        // 로딩 중에 "방이 없어요"가 한 프레임 스치면 사용자는 조건을 잘못 골랐다고 오해한다.
        assertNull(state(isLoading = true).emptyReason)
        assertNull(state(items = listOf(exploreChallenge("c1"))).emptyReason)
    }

    @Test
    fun `명세의 빈 사유 네 가지가 모두 나올 수 있다`() {
        // 사유가 늘었는데 어느 상태로도 도달하지 못하면 그 문구는 영원히 죽은 코드다.
        val reached =
            setOf(
                state(sort = ExploreSort.COMPLETION_RATE).emptyReason,
                state(filter = ExploreFilter(eligibleOnly = true)).emptyReason,
                state(filter = ExploreFilter(categories = setOf(Category.EXERCISE))).emptyReason,
                state().emptyReason,
            )

        assertEquals(EmptyReason.entries.toSet(), reached)
    }

    @Test
    fun `커서가 남아 있고 아무것도 불러오는 중이 아닐 때만 더 불러올 수 있다`() {
        // 이 셋 중 하나만 틀려도 같은 커서를 겹쳐 보내 목록에 같은 카드가 두 번 붙는다.
        assertTrue(state(nextCursor = "cur1").canLoadMore)
        assertFalse(state(nextCursor = null).canLoadMore)
        assertFalse(state(nextCursor = "cur1", isLoading = true).canLoadMore)
        assertFalse(state(nextCursor = "cur1", isLoadingMore = true).canLoadMore)
    }
}

private fun state(
    isLoading: Boolean = false,
    isLoadingMore: Boolean = false,
    filter: ExploreFilter = ExploreFilter.none,
    sort: ExploreSort = ExploreSort.default,
    items: List<ExploreChallenge> = emptyList(),
    nextCursor: String? = null,
) = ExploreListState(
    isLoading = isLoading,
    isLoadingMore = isLoadingMore,
    filter = filter,
    sort = sort,
    items = items,
    nextCursor = nextCursor,
    errorMessage = null,
    loadMoreFailed = false,
)
