package com.ruleup.domain.time

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/** 표시 기준일은 서버 판정과 같은 KST 여야 한다. */
class ServiceDateTest {
    @Test
    fun `기기 시간대가 어디든 서울 기준 날짜를 돌려준다`() {
        assertEquals(LocalDate.now(ZoneId.of("Asia/Seoul")), ServiceDate.today())
    }
}
