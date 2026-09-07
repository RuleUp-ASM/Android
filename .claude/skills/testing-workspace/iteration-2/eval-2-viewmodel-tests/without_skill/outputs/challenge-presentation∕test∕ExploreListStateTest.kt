package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.presentation.fake.challenges
import com.ruleup.domain.entity.category.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 상태에서 파생되는 두 값만 본다. 빈 결과 사유는 **문구와 다음 행동(CTA)을 가르는 분기**라
 * 우선순위가 어긋나면 사용자가 못 고치는 조건을 고치라고 안내하게 된다.
 */
class ExploreListStateTest {
    private fun state(
        isLoading: Boolean = false,
        isLoadingMore: Boolean = false,
        filter: ExploreFilter = ExploreFilter.none,
        sort: ExploreSort = ExploreSort.POPULAR,
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

    @Test
    fun `첫 프레임은 빈 목록이 아니라 로딩이다`() {
        // 초기값이 로딩이 아니면 첫 응답 전에 빈 결과 문구가 한 번 번쩍인다.
        assertTrue(ExploreListState.initial.isLoading)
        assertNull(ExploreListState.initial.emptyReason)
    }

    @Test
    fun `결과가 있으면 빈 사유가 없다`() {
        assertNull(state(items = challenges("a")).emptyReason)
    }

    @Test
    fun `지표 정렬의 0건은 조건이 좁아서가 아니라 기록이 없어서다`() {
        // 표본 미달 방은 목록에서 아예 빠진다. 필터를 풀라고 안내하면 풀어도 결과가 안 나온다.
        val narrowed =
            state(
                sort = ExploreSort.COMPLETION_RATE,
                filter = ExploreFilter(categories = setOf(Category.EXERCISE), eligibleOnly = true),
            )

        assertEquals(EmptyReason.LOW_SAMPLE, narrowed.emptyReason)
    }

    @Test
    fun `티어 조건이 켜져 있으면 그 완화를 먼저 제안한다`() {
        val tierOnly = state(filter = ExploreFilter(categories = setOf(Category.EXERCISE), eligibleOnly = true))

        assertEquals(EmptyReason.TIER_FILTER, tierOnly.emptyReason)
    }

    @Test
    fun `필터만 걸려 있으면 필터 때문에 비었다고 본다`() {
        assertEquals(EmptyReason.FILTERED, state(filter = ExploreFilter(categories = setOf(Category.MIND))).emptyReason)
    }

    @Test
    fun `아무 조건도 없이 0건이면 이 분류에 방이 없는 것이다`() {
        assertEquals(EmptyReason.CATEGORY_EMPTY, state().emptyReason)
    }

    @Test
    fun `커서가 있어도 요청이 도는 중이면 더 받지 않는다`() {
        assertTrue(state(nextCursor = "cur1").canLoadMore)
        assertFalse(state(nextCursor = "cur1", isLoading = true).canLoadMore)
        assertFalse(state(nextCursor = "cur1", isLoadingMore = true).canLoadMore)
        assertFalse(state(nextCursor = null).canLoadMore)
    }
}
