package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeSignalTest {
    @Test
    fun `잠금해제도 화면 켜짐도 없으면 보낼 것이 없다`() {
        // 빈 WAKE 를 배치에 실으면 "아직 안 일어났다"가 신호로 나가 서버가 판정 입력으로 읽는다.
        val wake = VerificationSignal.Wake(firstUnlock = null, firstScreenOn = null, deviceSecure = true)

        assertTrue(wake.isEmpty)
    }

    @Test
    fun `화면 켜짐만 있어도 보낼 것이 있다`() {
        // 잠금을 안 건 기기는 firstUnlock 이 영영 안 나온다. 이걸 빈 신호로 접으면
        // 그런 기기의 기상 인증이 통째로 판정 불가가 된다 — 폴백이 서버에 닿지 못한다.
        val wake = VerificationSignal.Wake(firstUnlock = null, firstScreenOn = 1L, deviceSecure = false)

        assertFalse(wake.isEmpty)
    }
}
