package com.ruleup.domain.entity.category

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryTest {
    @Test
    fun `12종이 관심 분야 정책 순서대로 정의돼 있다`() {
        // 온보딩 관심사 화면과 탐색 카테고리 그리드가 entries 를 그대로 그리므로 순서가 곧 계약이다.
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
    fun `탐색 API 의 구 code 는 별칭으로 흡수한다`() {
        // 서버 정렬 전까지 탐색·카테고리 API 가 내려주는 값. 별칭이 없으면 이 둘만 조용히 사라진다.
        assertEquals(Category.HOUSEKEEPING, Category.fromValue("TIDYING"))
        assertEquals(Category.CAREER_PRODUCTIVITY, Category.fromValue("CAREER"))
    }

    @Test
    fun `모르는 code 는 null 이다`() {
        assertNull(Category.fromValue("COOKING"))
        assertNull(Category.fromValue(""))
    }

    @Test
    fun `toCategories 는 모르는 값을 걸러낸다`() {
        assertEquals(
            listOf(Category.EXERCISE, Category.READING),
            listOf("EXERCISE", "CODING", "READING").toCategories(),
        )
        assertEquals(emptyList(), null.toCategories())
    }
}
