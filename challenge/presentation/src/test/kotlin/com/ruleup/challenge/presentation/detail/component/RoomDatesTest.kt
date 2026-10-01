package com.ruleup.challenge.presentation.detail.component

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** 방 피드의 날짜·시각 표기. */
class RoomDatesTest {
    @Test
    fun `UTC 로 온 시각을 KST 로 옮겨 보여준다`() {
        assertEquals("10:17", feedTimeLabel("2026-09-22T01:17:00Z"))
    }

    @Test
    fun `KST 오프셋으로 온 시각은 그대로 보여준다`() {
        assertEquals("10:17", feedTimeLabel("2026-09-22T10:17:00+09:00"))
    }

    @Test
    fun `자정 경계에서 날짜가 하루 밀리지 않는다`() {
        assertEquals("오늘", feedDateHeader("2026-09-22T16:00:00Z", today = LocalDate.of(2026, 9, 23)))
        assertEquals("2026-09-23", feedDateKey("2026-09-22T16:00:00Z"))
    }

    @Test
    fun `오프셋이 없는 값은 이미 서비스 기준으로 보고 그대로 쓴다`() {
        // 파싱에 실패했다고 빈칸으로 두면 멀쩡한 피드가 시각 없이 그려진다.
        assertEquals("10:17", feedTimeLabel("2026-09-22T10:17:00"))
        assertEquals("2026-09-22", feedDateKey("2026-09-22T10:17:00"))
    }

    @Test
    fun `기간은 시작일과 종료일을 모두 센다`() {
        assertEquals("9.1 – 9.7 · 1주 0일", periodLabel("2026-09-01", "2026-09-07"))
        assertEquals("9.1 – 9.1 · 1일", periodLabel("2026-09-01", "2026-09-01"))
    }

    @Test
    fun `개월은 일수가 아니라 달력으로 센다`() {
        // 2월은 28일이라 30일 단위로 나누면 한 달이 되지 않는다.
        assertEquals("2.1 – 2.28 · 1개월 0일", periodLabel("2026-02-01", "2026-02-28"))
        assertEquals("1.1 – 12.31 · 1년 0일", periodLabel("2026-01-01", "2026-12-31"))
    }

    @Test
    fun `모든 단위가 있으면 년부터 일까지 나열한다`() {
        assertEquals("1.1 – 2.8 · 1년 1개월 1주 1일", periodLabel("2026-01-01", "2027-02-08"))
    }

    @Test
    fun `0인 중간 단위는 숨긴다`() {
        assertEquals("1.1 – 1.3 · 1년 3일", periodLabel("2026-01-01", "2027-01-03"))
        assertEquals("9.1 – 10.10 · 1개월 1주 3일", periodLabel("2026-09-01", "2026-10-10"))
    }

    @Test
    fun `종료일이 시작일보다 앞서면 날짜 범위만 보여준다`() {
        assertEquals("9.7 – 9.1", periodLabel("2026-09-07", "2026-09-01"))
    }
}
