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
 * 상태에서 파생되는 두 규칙 — "0건일 때 무슨 이유로 비었는가"와 "지금 더 부를 수 있는가" — 만 본다.
 *
 * 사유마다 화면 문구와 CTA 가 달라서, 여기서 잘못 갈리면 사용자는 풀 수 있는 조건(티어 컷)을 놔둔 채
 * "방이 없구나" 하고 나간다. 조회 흐름은 `ExploreListViewModelTest`(모듈 층)가 본다.
 */
class ExploreListEmptyReasonTest {
    @Test
    fun `아직 불러오는 중이면 빈 이유를 정하지 않는다`() {
        // 로딩 중 0건은 "결과가 없다"가 아니다. 문구를 먼저 띄우면 목록이 오면서 깜빡인다.
        assertNull(state(isLoading = true).emptyReason)
    }

    @Test
    fun `결과가 하나라도 있으면 빈 이유가 없다`() {
        assertNull(state(items = listOf(exploreChallenge("a"))).emptyReason)
    }

    @Test
    fun `지표 정렬에서 0건이면 조건이 좁아서가 아니라 기록이 부족해서라고 안내한다`() {
        // 지표 정렬은 표본 미달 방을 목록에서 아예 빼므로, 필터를 풀어도 결과가 늘지 않는다.
        assertEquals(
            EmptyReason.LOW_SAMPLE,
            state(
                sort = ExploreSort.COMPLETION_RATE,
                filter = ExploreFilter(categories = setOf(Category.EXERCISE), eligibleOnly = true),
            ).emptyReason,
        )
    }

    @Test
    fun `티어 조건이 켜진 채 0건이면 그 조건을 먼저 풀라고 안내한다`() {
        // 티어 컷은 초기 풀을 가장 크게 깎는다 — 다른 필터보다 먼저 제안해야 결과가 돌아온다.
        assertEquals(
            EmptyReason.TIER_FILTER,
            state(filter = ExploreFilter(categories = setOf(Category.EXERCISE), eligibleOnly = true)).emptyReason,
        )
    }

    @Test
    fun `티어 말고 다른 필터가 걸린 채 0건이면 필터 때문이라고 안내한다`() {
        assertEquals(
            EmptyReason.FILTERED,
            state(filter = ExploreFilter(categories = setOf(Category.EXERCISE))).emptyReason,
        )
    }

    @Test
    fun `아무 필터도 없이 0건이면 이 분야에 방이 없다고 안내한다`() {
        assertEquals(EmptyReason.CATEGORY_EMPTY, state().emptyReason)
    }
}

class ExploreListCanLoadMoreTest {
    @Test
    fun `커서가 없으면 마지막 페이지이므로 더 부르지 않는다`() {
        assertFalse(state(nextCursor = null).canLoadMore)
    }

    @Test
    fun `첫 페이지를 불러오는 중에는 다음 페이지를 부르지 않는다`() {
        assertFalse(state(isLoading = true, nextCursor = "cursor-1").canLoadMore)
    }

    @Test
    fun `이미 다음 페이지를 부르는 중에는 또 부르지 않는다`() {
        assertFalse(state(isLoadingMore = true, nextCursor = "cursor-1").canLoadMore)
    }

    @Test
    fun `쉬고 있고 커서가 남아 있으면 다음 페이지를 부를 수 있다`() {
        assertTrue(state(nextCursor = "cursor-1").canLoadMore)
    }
}

class ExploreListInitialStateTest {
    @Test
    fun `처음 화면은 조회 중이고 필터도 정렬도 기본값이다`() {
        // 초기값이 로딩이 아니면 첫 진입에 빈 목록 문구가 한 프레임 스친다.
        val initial = ExploreListState.initial

        assertTrue(initial.isLoading)
        assertEquals(ExploreFilter.none, initial.filter)
        assertEquals(ExploreSort.default, initial.sort)
        assertTrue(initial.items.isEmpty())
        assertNull(initial.nextCursor)
    }
}

private fun state(
    isLoading: Boolean = false,
    isLoadingMore: Boolean = false,
    filter: ExploreFilter = ExploreFilter.none,
    sort: ExploreSort = ExploreSort.default,
    items: List<ExploreChallenge> = emptyList(),
    nextCursor: String? = null,
) = ExploreListState.initial.copy(
    isLoading = isLoading,
    isLoadingMore = isLoadingMore,
    filter = filter,
    sort = sort,
    items = items,
    nextCursor = nextCursor,
)

/**
 * 사유마다 그것을 고정하는 테스트 이름. 사유가 늘면 이 `when` 이 컴파일되지 않아 누락이 드러난다 —
 * 새 사유는 문구와 CTA 를 하나씩 더 만들기 때문에 조용히 늘면 화면이 아무 말도 못 하는 상태가 생긴다.
 */
private fun EmptyReason.coveredBy(): String =
    when (this) {
        EmptyReason.FILTERED -> "티어 말고 다른 필터가 걸린 채 0건이면 필터 때문이라고 안내한다"
        EmptyReason.TIER_FILTER -> "티어 조건이 켜진 채 0건이면 그 조건을 먼저 풀라고 안내한다"
        EmptyReason.LOW_SAMPLE -> "지표 정렬에서 0건이면 조건이 좁아서가 아니라 기록이 부족해서라고 안내한다"
        EmptyReason.CATEGORY_EMPTY -> "아무 필터도 없이 0건이면 이 분야에 방이 없다고 안내한다"
    }
