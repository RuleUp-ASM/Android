package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExploreSortTest {
    @Test
    fun `명세의 6종만 정의돼 있고 기본은 인기순이다`() {
        // 정의 밖 값이 섞이면 서버가 400 INVALID_SORT_TYPE 으로 막는다.
        assertEquals(
            listOf("POPULAR", "PARTICIPANTS", "COMPLETION_RATE", "SUCCESS_FAIL_RATIO", "LATEST", "DEADLINE"),
            ExploreSort.entries.map { it.value },
        )
        assertEquals(ExploreSort.POPULAR, ExploreSort.default)
    }

    @Test
    fun `지표 정렬만 표본 미달 방을 제외한다`() {
        // 이 구분이 무너지면 화면이 "조건에 맞는 방이 없어요"로 잘못 안내한다.
        assertTrue(ExploreSort.COMPLETION_RATE.excludesLowSample)
        assertTrue(ExploreSort.SUCCESS_FAIL_RATIO.excludesLowSample)
        assertFalse(ExploreSort.POPULAR.excludesLowSample)
        assertFalse(ExploreSort.DEADLINE.excludesLowSample)
    }

    @Test
    fun `모르는 정렬 값은 인기순으로 떨어진다`() {
        assertEquals(ExploreSort.POPULAR, ExploreSort.fromValue("TEMPLATE_USAGE"))
        assertEquals(ExploreSort.POPULAR, ExploreSort.fromValue(null))
    }
}
