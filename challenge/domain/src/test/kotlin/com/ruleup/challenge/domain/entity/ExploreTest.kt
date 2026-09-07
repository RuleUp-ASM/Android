package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExploreSortTest {
    @Test
    fun `명세의 6종만 정의돼 있고 기본은 인기순이다`() {
        // 구 TEMPLATE_USAGE·TRENDING 이 남아 있으면 서버가 400 INVALID_SORT_TYPE 으로 막는다.
        assertEquals(
            listOf("POPULAR", "PARTICIPANTS", "COMPLETION_RATE", "SUCCESS_FAIL_RATIO", "RECENT", "DEADLINE"),
            ExploreSort.entries.map { it.value },
        )
        assertEquals(ExploreSort.POPULAR, ExploreSort.default)
    }

    @Test
    fun `지표 정렬만 표본 미달 방을 제외한다`() {
        // 빈 결과 문구를 가르는 기준이다 — "조건이 좁다"가 아니라 "기록이 없다"로 안내해야 한다.
        assertTrue(ExploreSort.COMPLETION_RATE.excludesLowSample)
        assertTrue(ExploreSort.SUCCESS_FAIL_RATIO.excludesLowSample)
        assertFalse(ExploreSort.POPULAR.excludesLowSample)
        assertFalse(ExploreSort.DEADLINE.excludesLowSample)
    }

    @Test
    fun `모르는 정렬 값은 null 이다`() {
        assertNull(ExploreSort.fromValue("TEMPLATE_USAGE"))
        assertNull(ExploreSort.fromValue(null))
    }
}

class ExploreFilterTest {
    @Test
    fun `카테고리는 csv 로 직렬화된다`() {
        val filter = ExploreFilter(categories = setOf(Category.EXERCISE, Category.READING))

        val param = filter.categoriesParam()

        assertEquals(setOf("EXERCISE", "READING"), param!!.split(",").toSet())
    }

    @Test
    fun `카테고리를 고르지 않으면 파라미터를 보내지 않는다`() {
        // 빈 문자열을 보내면 서버가 "빈 카테고리"로 읽어 0건이 될 수 있다 — 아예 빼야 전체가 된다.
        assertNull(ExploreFilter.none.categoriesParam())
    }

    @Test
    fun `티어 컷은 기본이 꺼짐이다`() {
        // 켜 두면 초기 풀이 작아 빈 결과가 급증한다(정책 가드레일).
        assertFalse(ExploreFilter.none.eligibleOnly)
        assertEquals(0, ExploreFilter.none.activeCount)
    }

    @Test
    fun `카테고리는 몇 개를 골랐든 배지 하나로 센다`() {
        val filter =
            ExploreFilter(
                categories = setOf(Category.EXERCISE, Category.READING, Category.STUDY),
                verifyType = VerificationType.AUTO,
                eligibleOnly = true,
            )

        assertEquals(3, filter.activeCount)
    }
}
