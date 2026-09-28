package com.ruleup.android_ruleup.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 푸시 표시 판정. */
class PushMessageTest {
    @Test
    fun `식별자가 없으면 표시하지 않는다`() {
        assertNull(PushMessage.from(data = emptyMap(), title = "제목", body = "본문"))
    }

    @Test
    fun `제목이 없으면 빈 알림을 만들지 않는다`() {
        assertNull(PushMessage.from(data = mapOf("notification_id" to "n1"), title = null, body = "본문"))
    }

    @Test
    fun `식별자와 제목이 있으면 표시한다`() {
        val push =
            PushMessage.from(
                data = mapOf("notification_id" to "n1", "deeplink" to "ruleup://mypage/tier"),
                title = "다이아 승급까지 5점",
                body = null,
            )

        assertEquals("n1", push?.notificationId)
        assertEquals("ruleup://mypage/tier", push?.deeplink)
        // 본문이 없어도 제목만으로 알림이 성립한다.
        assertEquals("", push?.body)
    }

    @Test
    fun `딥링크가 비면 없는 것으로 본다`() {
        val push =
            PushMessage.from(
                data = mapOf("notification_id" to "n1", "deeplink" to "  "),
                title = "제목",
                body = "본문",
            )

        assertNull(push?.deeplink)
    }
}
