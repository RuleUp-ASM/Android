package com.ruleup.domain.entity.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 연결된 소셜 제공자. */
class SocialProviderTest {
    @Test
    fun `명세의 2종이며 서버 값은 대문자다`() {
        assertEquals(listOf("KAKAO", "GOOGLE"), SocialProvider.entries.map { it.value })
    }

    @Test
    fun `모르는 제공자는 아무 쪽으로도 접지 않는다`() {
        assertNull(SocialProvider.fromValue("APPLE"))
        assertNull(SocialProvider.fromValue("kakao"))
        assertNull(SocialProvider.fromValue(null))
    }
}
