package com.ruleup.profile.data.dto

import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.profile.domain.entity.DayItemStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `월 캘린더의 오늘과 검사중이 상태로 살아 있다`() {
        // 종전에는 이 두 값을 몰라 NOT_TARGET 으로 접었고, 오늘 날짜가 "판정 대상 아님"으로 비어 보였다.
        val payload =
            """
            {"month":"2026-07","days":[
              {"date":"2026-07-24","status":"CHECKING","successCount":2,"targetCount":3},
              {"date":"2026-07-25","status":"IN_PROGRESS","successCount":1,"targetCount":3}]}
            """.trimIndent()

        val calendar = json.decodeFromString<ActivityCalendarResponse>(payload).toDomain()

        assertEquals(CalendarDayStatus.CHECKING, calendar.days.first().status)
        assertEquals(CalendarDayStatus.IN_PROGRESS, calendar.days.last().status)
    }

    @Test
    fun `모르는 일자 상태는 칠하지 않는다`() {
        // 특정 상태로 접으면 그 날짜에 대해 거짓말을 한다 — 표기를 생략하는 편이 낫다.
        val payload = """{"month":"2026-07","days":[{"date":"2026-07-24","status":"FUTURE_VALUE"}]}"""

        val calendar = json.decodeFromString<ActivityCalendarResponse>(payload).toDomain()

        assertNull(calendar.days.single().status)
    }

    @Test
    fun `일자 상세의 성공과 이의 조건이 매핑된다`() {
        // 종전에는 DONE 을 몰라 PENDING 으로 접었고, 성공한 인증이 "판정 대기"로 보였다.
        val payload =
            """
            {"date":"2026-07-20","items":[
              {"challengeId":"c_301","title":"매일 아침 6시 기상","verificationId":"v_9911","status":"FAILED",
               "confirmedAt":"2026-07-21T00:00:00+09:00","failureReason":"WOKE_UP_LATE",
               "appeal":{"eligible":true,"eligibleUntil":"2026-07-22T00:00:00+09:00","remainingThisMonth":2}},
              {"challengeId":"c_120","title":"하루 30분 독서","verificationId":"v_9812","status":"DONE",
               "verifiedVia":"MANUAL","confirmedAt":"2026-07-20T21:43:00+09:00"}]}
            """.trimIndent()

        val detail = json.decodeFromString<CalendarDayDetailResponse>(payload).toDomain()

        val failed = detail.items.first()
        assertEquals(DayItemStatus.FAILED, failed.status)
        // 이 키가 없으면 실패 항목에서 이의로 들어갈 경로가 없다.
        assertEquals("v_9911", failed.verificationId)
        assertEquals("2026-07-22T00:00:00+09:00", failed.appeal?.eligibleUntil)

        val done = detail.items.last()
        assertEquals(DayItemStatus.DONE, done.status)
        assertEquals("2026-07-20T21:43:00+09:00", done.confirmedAt)
        // 실패가 아니면 이의 블록이 없다.
        assertNull(done.appeal)
    }
}
