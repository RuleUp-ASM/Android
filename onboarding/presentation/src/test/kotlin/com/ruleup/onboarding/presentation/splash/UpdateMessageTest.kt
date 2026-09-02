package com.ruleup.onboarding.presentation.splash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 강제 업데이트 안내 문구. 정책은 서버가 정하므로 **받은 버전을 그대로 넣고, 못 받았으면
 * 지어내지 않는다** — 없는 버전을 적으면 사용자가 스토어에서 찾다 못 찾는다.
 */
class UpdateMessageTest {
    @Test
    fun `버전을 받으면 그 버전 이상으로 올리라고 말한다`() {
        assertEquals("1.2.0 이상으로 업데이트해 주세요.", updateMessage("1.2.0"))
    }

    @Test
    fun `버전을 못 받으면 숫자를 지어내지 않는다`() {
        // 없는 버전을 적으면 사용자가 스토어에서 찾다 못 찾는다.
        val message = updateMessage(null)

        assertTrue(message.contains("최신 버전"))
        assertTrue(message.none { it.isDigit() }, message)
    }

    @Test
    fun `빈 문자열도 못 받은 것으로 다룬다`() {
        // 서버가 필드를 비워 보내는 배포본에서 " 이상으로 업데이트해 주세요" 가 뜨면 안 된다.
        assertEquals(updateMessage(null), updateMessage("   "))
    }
}
