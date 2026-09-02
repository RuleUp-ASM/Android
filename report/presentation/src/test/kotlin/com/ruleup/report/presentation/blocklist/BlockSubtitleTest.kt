package com.ruleup.report.presentation.blocklist

import com.ruleup.report.presentation.blocklist.fake.blockedChallenge
import com.ruleup.report.presentation.blocklist.fake.blockedUser
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockSubtitleTest {
    @Test
    fun `차단일은 연도를 떼고 월 일로 적는다`() {
        assertEquals("8.28 차단", blockedUser().subtitle())
    }

    @Test
    fun `차단일이 없으면 날짜 자리를 비우고 차단됨만 남긴다`() {
        // 서버가 값을 안 주면 "언제"만 빠지고 해제는 그대로 된다.
        assertEquals("차단됨", blockedUser(at = null).subtitle())
    }

    @Test
    fun `형식이 깨진 날짜는 그대로 흘리지 않고 차단됨으로 접는다`() {
        // ISO 가 아닌 문자열을 그대로 그리면 화면에 서버 내부 값이 노출된다.
        assertEquals("차단됨", blockedUser(at = "어제").subtitle())
    }

    @Test
    fun `참여 중인 챌린지는 참여 여부를 날짜 앞에 붙인다`() {
        assertEquals("참여 중 · 8.30 차단", blockedChallenge(participating = true).subtitle())
    }

    @Test
    fun `참여하지 않는 챌린지는 날짜만 적는다`() {
        assertEquals("8.30 차단", blockedChallenge(participating = false).subtitle())
    }
}
