package com.ruleup.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/** KST 자정 경계. */
class ServiceDateBoundaryTest {
    @Test
    fun `자정 직전과 직후에 날짜가 하루 넘어간다`() {
        val justBefore = Instant.parse("2026-09-22T14:59:59Z")
        val justAfter = Instant.parse("2026-09-22T15:00:00Z")

        assertEquals(LocalDate.of(2026, 9, 22), justBefore.atZone(ServiceDate.ZONE).toLocalDate())
        assertEquals(LocalDate.of(2026, 9, 23), justAfter.atZone(ServiceDate.ZONE).toLocalDate())
    }

    @Test
    fun `기기 타임존이 달라도 서비스 기준일은 KST 를 따른다`() {
        val moment = Instant.parse("2026-09-22T15:00:00Z")

        assertEquals(LocalDate.of(2026, 9, 22), moment.atZone(ZoneId.of("America/Los_Angeles")).toLocalDate())
        assertEquals(LocalDate.of(2026, 9, 23), moment.atZone(ServiceDate.ZONE).toLocalDate())
    }

    @Test
    fun `UTC 로 온 시각도 KST 기준일로 옮긴다`() {
        // 서버 응답에 Z 표기가 섞여 있다.
        val kst = ServiceDate.atZone("2026-09-22T16:00:00Z")

        assertEquals(LocalDate.of(2026, 9, 23), kst?.toLocalDate())
        assertEquals(1, kst?.hour)
    }
}
