package com.ruleup.home.presentation

import com.ruleup.verification.domain.entity.TodayStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 홈 구성 규칙(#584). 모든 챌린지가 오늘 꼭 해야 하는 건 아니다 — 주 N회를 「오늘 할 일」에 섞으면
 * 안 해도 되는 날에 밀린 일처럼 보인다.
 */
class HomeSectionsTest {
    @Test
    fun `주 7회는 매일 루틴이고 주 3회는 아니다`() {
        assertTrue(card("a", weeklyCount = 7).isDaily)
        assertFalse(card("b", weeklyCount = 3).isDaily)
    }

    @Test
    fun `주간 횟수를 모르면 매일 루틴으로 본다`() {
        // 주 N회로 접으면 오늘 해야 할 일이 「오늘」 목록에서 빠진다.
        assertTrue(card("a", weeklyCount = null).isDaily)
    }

    @Test
    fun `오늘 할 일에는 오늘 대상인 매일 루틴만 센다`() {
        val summary =
            homeSummary(
                listOf(
                    card("done", todayStatus = TodayStatus.DONE),
                    card("open"),
                    card("rest", todayTarget = false),
                    card("weekly", weeklyCount = 3),
                    card("upcoming", active = false),
                ),
            )

        assertEquals(HomeSummary(dailyDone = 1, dailyTotal = 2, weeklyTotal = 1), summary)
    }

    @Test
    fun `히어로에는 직접 체크하는 챌린지만 올린다`() {
        // 자동 인증은 사용자가 할 게 없다.
        val cards = listOf(card("auto"), card("manual"))

        val hero = pickHero(cards, mapOf("auto" to false, "manual" to true))

        assertEquals("manual", hero?.challengeId)
    }

    @Test
    fun `인증 방식을 아직 모르면 히어로를 띄우지 않는다`() {
        assertNull(pickHero(listOf(card("a")), manualCheckable = emptyMap()))
    }

    @Test
    fun `오늘 대상인 매일 루틴이 주 N회보다 먼저 히어로가 된다`() {
        val cards = listOf(card("weekly", weeklyCount = 3), card("daily"))

        val hero = pickHero(cards, mapOf("weekly" to true, "daily" to true))

        assertEquals("daily", hero?.challengeId)
    }

    @Test
    fun `오늘 이미 끝났거나 시작 전인 챌린지는 히어로 후보가 아니다`() {
        val cards =
            listOf(
                card("done", todayStatus = TodayStatus.DONE),
                card("failed", todayStatus = TodayStatus.FAILED),
                card("upcoming", active = false),
                card("not-today", todayTarget = false),
            )

        assertTrue(heroCandidates(cards).isEmpty())
    }

    @Test
    fun `주 N회가 오늘 대상이 아니면 이번 주를 다 채운 것으로 보고 후보에서 뺀다`() {
        val cards =
            listOf(
                card("filled", weeklyCount = 3, todayStatus = TodayStatus.NOT_TARGET),
                card("open", weeklyCount = 3, todayTarget = false),
            )

        assertEquals(listOf("open"), heroCandidates(cards).map { it.challengeId })
    }

    @Test
    fun `인사는 시간대마다 바뀐다`() {
        assertEquals("좋은 아침이에요", greeting(5))
        assertEquals("좋은 오후예요", greeting(11))
        assertEquals("좋은 저녁이에요", greeting(17))
        assertEquals("편안한 밤이에요", greeting(22))
        assertEquals("편안한 밤이에요", greeting(4))
    }

    private fun card(
        id: String,
        weeklyCount: Int? = 7,
        todayTarget: Boolean = true,
        todayStatus: TodayStatus? = TodayStatus.IN_PROGRESS,
        active: Boolean = true,
    ) = HomeChallengeUi(
        challengeId = id,
        title = "챌린지 $id",
        subtitle = "진행중 · 솔로",
        progress = 0f,
        todayTarget = todayTarget,
        category = null,
        weeklyCount = weeklyCount,
        todayStatus = todayStatus,
        imageUrl = null,
        active = active,
    )
}
