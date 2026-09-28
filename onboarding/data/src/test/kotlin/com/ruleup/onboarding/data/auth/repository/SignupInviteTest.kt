package com.ruleup.onboarding.data.auth.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignupInviteTest {
    @Test
    fun `설치 referrer의 초대 URL을 복원한다`() {
        assertEquals(
            "https://android.ruleup.co.kr/c/token",
            inviteLinkFromReferrer("ruleup_invite_type=c&ruleup_invite_url=https%3A%2F%2Fandroid.ruleup.co.kr%2Fc%2Ftoken"),
        )
        assertEquals(
            "https://android.ruleup.co.kr/inv/friend",
            inviteLinkFromReferrer("url=https%3A%2F%2Fandroid.ruleup.co.kr%2Finv%2Ffriend"),
        )
    }

    @Test
    fun `광고 referrer와 외부 도메인은 초대로 전달하지 않는다`() {
        assertNull(inviteLinkFromReferrer("utm_source=google-play"))
        assertNull(validInviteLink("https://evil.example/c/token"))
        assertNull(inviteLinkFromReferrer("url=%ZZ"))
    }
}
