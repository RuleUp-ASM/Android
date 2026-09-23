package com.ruleup.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * KST 자정 경계.
 *
 * 판정·마감·귀속일이 전부 서버 KST 하루 단위다. 화면이 **기기 타임존**으로 「오늘」을 정하면
 * 같은 순간을 두 날짜로 말하게 된다 — 해외 체류 중에 홈은 16일, 방은 17일 건을 오늘이라 가리키고
 * "이의 오늘까지"가 "내일까지"로 바뀐다(VER-13).
 *
 * 실제 자정을 기다릴 수 없으므로 경계 직전·직후의 순간을 고정해 검증한다.
 */
class ServiceDateBoundaryTest {
    @Test
    fun `자정 직전과 직후에 날짜가 하루 넘어간다`() {
        // UTC 14:59:59 = KST 23:59:59 (같은 날) / UTC 15:00:00 = KST 00:00:00 (다음 날)
        val justBefore = Instant.parse("2026-09-22T14:59:59Z")
        val justAfter = Instant.parse("2026-09-22T15:00:00Z")

        assertEquals(LocalDate.of(2026, 9, 22), justBefore.atZone(ServiceDate.ZONE).toLocalDate())
        assertEquals(LocalDate.of(2026, 9, 23), justAfter.atZone(ServiceDate.ZONE).toLocalDate())
    }

    @Test
    fun `기기 타임존이 달라도 서비스 기준일은 KST 를 따른다`() {
        // 같은 순간이 LA 에서는 아직 22일 오전이지만 서비스 기준으로는 이미 23일이다.
        val moment = Instant.parse("2026-09-22T15:00:00Z")

        assertEquals(LocalDate.of(2026, 9, 22), moment.atZone(ZoneId.of("America/Los_Angeles")).toLocalDate())
        assertEquals(LocalDate.of(2026, 9, 23), moment.atZone(ServiceDate.ZONE).toLocalDate())
    }

    @Test
    fun `UTC 로 온 시각도 KST 기준일로 옮긴다`() {
        // 서버 응답에 Z 표기가 섞여 있다. 문자열을 자르면 22일로 읽힌다.
        val kst = ServiceDate.atZone("2026-09-22T16:00:00Z")

        assertEquals(LocalDate.of(2026, 9, 23), kst?.toLocalDate())
        assertEquals(1, kst?.hour)
    }
}
