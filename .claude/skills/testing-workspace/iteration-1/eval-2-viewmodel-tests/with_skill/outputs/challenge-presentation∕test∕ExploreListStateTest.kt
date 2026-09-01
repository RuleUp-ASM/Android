package com.ruleup.challenge.presentation.explore.list.viewmodel

import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.presentation.fake.card
import com.ruleup.domain.entity.category.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 상태에서 파생되는 두 판단 — 빈 결과의 사유와 더 부를 수 있는지 — 만 본다. 둘 다 협력자 없이 값으로
 * 성립하므로 ViewModel 테스트가 다시 훑지 않는다.
 *
 * 사유를 틀리게 고르면 화면은 "필터를 풀어보세요"라고 말하는데 정작 필터는 걸려 있지 않은 상태가 된다 —
 * 사용자는 없는 조건을 찾아 헤매고, 빈 결과율 가드레일의 해석도 함께 어긋난다.
 */
class ExploreListStateTest {
    @Test
    fun `아직 불러오는 중이면 비었다고 말하지 않는다`() {
        assertNull(state(isLoading = true).emptyReason)
    }

    @Test
    fun `결과가 있으면 빈 사유가 없다`() {
        assertNull(state(items = listOf(card("c1"))).emptyReason)
    }

    @Test
    fun `지표 정렬로 0건이면 조건이 좁은 게 아니라 기록이 없는 것이다`() {
        // 완주율·성공률 정렬은 표본 미달 방을 목록에서 아예 뺀다. 필터를 풀라고 안내하면 아무것도 달라지지 않는다.
        assertEquals(EmptyReason.LOW_SAMPLE, state(sort = ExploreSort.COMPLETION_RATE).emptyReason)
        assertEquals(EmptyReason.LOW_SAMPLE, state(sort = ExploreSort.SUCCESS_FAIL_RATIO).emptyReason)
    }

    @Test
    fun `티어 조건이 켜진 채 0건이면 그 조건부터 풀라고 말한다`() {
        val reason = state(filter = ExploreFilter(eligibleOnly = true)).emptyReason

        assertEquals(EmptyReason.TIER_FILTER, reason)
    }

    @Test
    fun `다른 필터로 0건이면 필터 때문이라고 말한다`() {
        val reason = state(filter = ExploreFilter(categories = setOf(Category.STUDY))).emptyReason

        assertEquals(EmptyReason.FILTERED, reason)
    }

    @Test
    fun `조건 없이 0건이면 이 카테고리에 방이 없는 것이다`() {
        assertEquals(EmptyReason.CATEGORY_EMPTY, state().emptyReason)
    }

    @Test
    fun `빈 결과 사유는 네 가지뿐이고 모두 실제 상태에서 나온다`() {
        // 사유가 늘면 이 단언이 깨진다 — 문구와 CTA 를 정하지 않은 사유가 화면에 새는 걸 막는다.
        val produced =
            listOf(
                state(sort = ExploreSort.SUCCESS_FAIL_RATIO),
                state(filter = ExploreFilter(eligibleOnly = true)),
                state(filter = ExploreFilter(categories = setOf(Category.STUDY))),
                state(),
            ).mapNotNull { it.emptyReason }

        assertEquals(EmptyReason.entries.toSet(), produced.toSet())
    }

    @Test
    fun `마지막 페이지이거나 이미 부르는 중이면 더 부르지 않는다`() {
        // 하단 근접은 스크롤마다 올라온다. 여기서 막지 못하면 같은 커서로 요청이 연달아 나간다.
        assertTrue(state(nextCursor = "cur1", isLoading = false).canLoadMore)
        assertFalse(state(nextCursor = null, isLoading = false).canLoadMore)
        assertFalse(state(nextCursor = "cur1", isLoading = true).canLoadMore)
        assertFalse(state(nextCursor = "cur1", isLoading = false, isLoadingMore = true).canLoadMore)
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
