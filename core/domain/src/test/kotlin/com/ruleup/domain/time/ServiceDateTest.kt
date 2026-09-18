package com.ruleup.domain.time

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 표시 기준일은 서버 판정과 같은 KST 여야 한다. 기기 타임존을 따르면 해외 체류 중에 홈과 방이
 * 서로 다른 날을 "오늘"이라 가리키고, 이의 기한 안내가 하루 어긋난다.
 */
class ServiceDateTest {
    @Test
    fun `기기 시간대가 어디든 서울 기준 날짜를 돌려준다`() {
        // 리마(GMT-5)는 서울보다 14시간 늦다 — 서울이 이미 다음 날인 시간대가 하루 중 14시간이다.
        assertEquals(LocalDate.now(ZoneId.of("Asia/Seoul")), ServiceDate.today())
    }
}
