package com.ruleup.domain.entity.category

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryTest {
    @Test
    fun `12종이 관심 분야 정책 순서대로 정의돼 있다`() {
        assertEquals(
            listOf(
                "EXERCISE",
                "WAKE_SLEEP",
                "DIET_HEALTH",
                "STUDY",
                "READING",
                "MIND",
                "FINANCE",
                "HOBBY",
                "HOUSEKEEPING",
                "CAREER_PRODUCTIVITY",
                "DETOX",
                "ETC",
            ),
            Category.entries.map { it.value },
        )
    }

    @Test
    fun `code 로 카테고리를 찾는다`() {
        assertEquals(Category.WAKE_SLEEP, Category.fromValue("WAKE_SLEEP"))
        assertEquals(Category.CAREER_PRODUCTIVITY, Category.fromValue("CAREER_PRODUCTIVITY"))
    }

    @Test
    fun `12종 확정 전 표기는 더 이상 흡수하지 않는다`() {
        assertNull(Category.fromValue("TIDYING"))
        assertNull(Category.fromValue("CAREER"))
    }

    @Test
    fun `15종 시절 code 도 별칭으로 흡수한다`() {
        assertEquals(Category.WAKE_SLEEP, Category.fromValue("WAKE_UP"))
        assertEquals(Category.DIET_HEALTH, Category.fromValue("HEALTH"))
        assertEquals(Category.MIND, Category.fromValue("MEDITATION"))
        assertEquals(Category.HOUSEKEEPING, Category.fromValue("COOKING"))
        assertEquals(Category.CAREER_PRODUCTIVITY, Category.fromValue("WORK"))
    }

    @Test
    fun `사라진 분류는 여전히 null 이다`() {
        // 대응되는 12종이 없다.
        assertNull(Category.fromValue("ENVIRONMENT"))
        assertNull(Category.fromValue("MUSIC"))
        assertNull(Category.fromValue("CODING"))
        assertNull(Category.fromValue(""))
    }

    @Test
    fun `toCategories 는 모르는 값을 걸러낸다`() {
        assertEquals(
            listOf(Category.EXERCISE, Category.READING),
            listOf("EXERCISE", "MUSIC", "READING").toCategories(),
        )
        assertEquals(emptyList(), null.toCategories())
    }
}
