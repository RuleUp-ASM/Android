package com.ruleup.challenge.presentation.targets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 대상 앱 고르기의 목록 좁히기. 사용자가 수백 개 중에서 찾아야 하므로, 못 찾으면 **아예 등록을
 * 포기한다** — 그러면 자동 인증이 성립하지 않는다.
 */
class FilterAppsTest {
    @Test
    fun `이름의 일부만 쳐도 찾는다`() {
        val found = filterApps(apps, query = "카카오", category = null)

        assertEquals(listOf("카카오톡"), found.map { it.label })
    }

    @Test
    fun `줄임말로는 찾지 못한다`() {
        // 부분 일치라 "카톡"은 "카카오톡"에 걸리지 않는다. 한국어 앱은 줄임말로 검색하는 일이
        // 흔해서 사용자가 못 찾고 등록을 포기할 수 있다 — 초성·줄임말 매칭을 넣을지는 기획
        // 판단이라 여기서는 현재 동작만 못 박는다.
        assertEquals(emptyList(), filterApps(apps, query = "카톡", category = null))
    }

    @Test
    fun `대소문자가 달라도 찾는다`() {
        // 영문 앱은 대문자로 시작하는 경우가 많은데 사용자는 소문자로 친다.
        assertEquals(listOf("Instagram"), filterApps(apps, query = "insta", category = null).map { it.label })
    }

    @Test
    fun `앞뒤 공백은 검색에 영향을 주지 않는다`() {
        // 키보드 자동완성이 공백을 붙이면 결과가 0건이 된다.
        assertEquals(listOf("Instagram"), filterApps(apps, query = "  Instagram  ", category = null).map { it.label })
    }

    @Test
    fun `검색어가 비면 거르지 않는다`() {
        assertEquals(apps.size, filterApps(apps, query = "   ", category = null).size)
    }

    @Test
    fun `카테고리를 고르면 그 카테고리만 남긴다`() {
        val found = filterApps(apps, query = "", category = "소셜")

        assertTrue(found.all { it.category == "소셜" })
    }

    @Test
    fun `카테고리를 선언하지 않은 앱은 특정 카테고리에서 빠진다`() {
        // 어디에도 속하지 않는다고 아무 데나 넣으면 목록이 거짓이 된다.
        val found = filterApps(apps, query = "", category = "소셜")

        assertTrue(found.none { it.label == "계산기" })
    }

    @Test
    fun `카테고리를 고르지 않으면 미분류 앱도 보인다`() {
        // "전체"에서까지 빠지면 그 앱은 영영 고를 수 없다.
        val found = filterApps(apps, query = "", category = null)

        assertTrue(found.any { it.label == "계산기" })
    }

    @Test
    fun `검색어와 카테고리는 함께 좁힌다`() {
        assertEquals(emptyList(), filterApps(apps, query = "계산", category = "소셜"))
    }

    private val apps =
        listOf(
            entry("카카오톡", "소셜"),
            entry("Instagram", "소셜"),
            entry("계산기", null),
        )

    private fun entry(
        label: String,
        category: String?,
    ) = AppEntry(
        packageName = "com.$label",
        label = label,
        icon = null,
        category = category,
        weeklyUsageMs = 0L,
    )
}
