package com.ruleup.domain.entity.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 연결된 소셜 제공자. 설정 허브가 이 값을 그대로 문구로 쓰므로, **틀린 값을 고르면 사용자에게
 * 거짓말이 된다** — 카카오로 가입한 사람에게 구글이라고 말하게 된다.
 */
class SocialProviderTest {
    @Test
    fun `명세의 2종이며 서버 값은 대문자다`() {
        // 로그인 요청의 소문자 어휘(OAuthProvider)와 다른 축이다. 섞으면 매칭이 조용히 실패한다.
        assertEquals(listOf("KAKAO", "GOOGLE"), SocialProvider.entries.map { it.value })
    }

    @Test
    fun `모르는 제공자는 아무 쪽으로도 접지 않는다`() {
        assertNull(SocialProvider.fromValue("APPLE"))
        assertNull(SocialProvider.fromValue("kakao"))
        assertNull(SocialProvider.fromValue(null))
    }
}
