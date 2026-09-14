package com.ruleup.support.presentation.common

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/** 서버는 UTC 로 준다. 문자열을 잘라 그리면 KST 사용자에게 9시간 이른 접수 시각이 보인다(#452). */
class InquiryFormatTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    @Test
    fun `UTC 접수 시각은 기기 시간대로 바꿔 그린다`() {
        assertEquals("09.14 21:34", shortDateTime("2026-09-14T12:34:10.123Z", seoul))
    }

    @Test
    fun `시간대를 넘기면 목록 날짜도 다음 날로 넘어간다`() {
        assertEquals("09.15", shortDate("2026-09-14T16:30:00Z", seoul))
    }

    @Test
    fun `읽을 수 없는 시각은 원문을 그대로 보여 준다`() {
        assertEquals("어제", shortDateTime("어제", seoul))
    }
}
