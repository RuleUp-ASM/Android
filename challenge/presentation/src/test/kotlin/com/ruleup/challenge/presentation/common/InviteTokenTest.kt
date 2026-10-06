package com.ruleup.challenge.presentation.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 초대 주소에서 앱 실행 파라미터에 실을 토큰을 꺼낸다. */
class InviteTokenTest {
    @Test
    fun `초대 주소의 마지막 경로를 토큰으로 꺼낸다`() {
        assertEquals("wtk_8f3a", inviteToken("https://android.ruleup.co.kr/w/wtk_8f3a"))
        assertEquals("cinv_1", inviteToken("https://android.ruleup.co.kr/c/cinv_1/?utm=kakao#x"))
    }

    @Test
    fun `토큰이 없는 주소면 앱 실행 파라미터를 싣지 않는다`() {
        // 빈 토큰으로 앱을 열면 수락 화면이 오류만 보인다 — 그때는 웹 주소로만 연다.
        assertNull(inviteToken("https://android.ruleup.co.kr/"))
        assertNull(inviteToken(""))
    }
}
