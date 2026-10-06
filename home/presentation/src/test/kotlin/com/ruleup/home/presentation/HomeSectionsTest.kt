package com.ruleup.home.presentation

import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.category.Category
import com.ruleup.profile.domain.entity.CalendarDayItem
import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.profile.domain.entity.DayItemStatus
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

    @Test
    fun `첫 챌린지는 관심 분야 것을 인기 순서대로 앞에 세운다`() {
        val trending = listOf(trend("a", Category.EXERCISE), trend("b", Category.READING), trend("c", null), trend("d", Category.READING))

        val picked = pickStarters(trending, interests = listOf(Category.READING))

        assertEquals(listOf("b", "d", "a", "c"), picked.map { it.challengeId })
    }

    @Test
    fun `첫 챌린지는 정해진 수까지만 고른다`() {
        val trending = (1..10).map { trend("t$it", Category.EXERCISE) }

        assertEquals(6, pickStarters(trending, interests = emptyList()).size)
    }

    @Test
    fun `그날을 다 지켰을 때만 체크 도장이다`() {
        assertEquals(DayStamp.DONE, dayStamp(MON, TUE, CalendarDayStatus.ALL_DONE))
    }

    @Test
    fun `일부만 지킨 날도 X 도장이다`() {
        // 하나라도 못 지켰으면 그날은 못 지킨 날로 보인다(사용자 결정 #596).
        assertEquals(DayStamp.FAILED, dayStamp(MON, TUE, CalendarDayStatus.PARTIAL))
        assertEquals(DayStamp.FAILED, dayStamp(MON, TUE, CalendarDayStatus.FAILED))
    }

    @Test
    fun `오늘은 판정 전이면 테두리 칸이고 다 지키면 체크다`() {
        assertEquals(DayStamp.TODAY, dayStamp(TUE, TUE, CalendarDayStatus.IN_PROGRESS))
        assertEquals(DayStamp.TODAY, dayStamp(TUE, TUE, null))
        assertEquals(DayStamp.DONE, dayStamp(TUE, TUE, CalendarDayStatus.ALL_DONE))
    }

    @Test
    fun `판정할 루틴이 없던 지난 날과 앞으로 올 날은 도장을 찍지 않는다`() {
        assertEquals(DayStamp.EMPTY, dayStamp(MON, TUE, null))
        assertEquals(DayStamp.FUTURE, dayStamp(TUE.plusDays(1), TUE, CalendarDayStatus.ALL_DONE))
    }

    @Test
    fun `그날 결과는 수행 실패 판정 중으로 갈리고 판정 전은 어느 쪽으로도 접지 않는다`() {
        val groups =
            listOf(
                dayItem("done", DayItemStatus.DONE),
                dayItem("failed", DayItemStatus.FAILED),
                dayItem("expected", DayItemStatus.FAIL_EXPECTED),
                dayItem("progress", DayItemStatus.IN_PROGRESS),
                dayItem("unknown", null),
            ).groupByResult()

        assertEquals(listOf("done"), groups.done.map { it.challengeId })
        assertEquals(listOf("failed", "expected"), groups.failed.map { it.challengeId })
        assertEquals(listOf("progress", "unknown"), groups.pending.map { it.challengeId })
    }

    private fun dayItem(
        id: String,
        status: DayItemStatus?,
    ) = CalendarDayItem(
        challengeId = id,
        title = "챌린지 $id",
        category = null,
        status = status,
        verificationId = null,
        verifiedVia = null,
        confirmedAt = null,
        failureReason = null,
        appeal = null,
    )

    private fun trend(
        id: String,
        category: Category?,
    ) = TrendingChallenge(
        rank = 1,
        challengeId = id,
        title = "챌린지 $id",
        imageUrl = null,
        category = category,
        participantCount = 10,
        recentJoins24h = 3,
        verificationType = VerificationType.MANUAL,
        minTier = null,
        joinable = true,
        endDate = null,
    )

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

    private companion object {
        val MON: java.time.LocalDate = java.time.LocalDate.of(2026, 10, 5)
        val TUE: java.time.LocalDate = java.time.LocalDate.of(2026, 10, 6)
    }
}
