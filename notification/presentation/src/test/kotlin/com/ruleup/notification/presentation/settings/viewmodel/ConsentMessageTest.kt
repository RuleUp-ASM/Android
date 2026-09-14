package com.ruleup.notification.presentation.settings.viewmodel

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/** 광고성 수신 동의·철회는 처리 일시를 알려야 한다. 시각이 빠지면 법정 고지가 성립하지 않는다. */
class ConsentMessageTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    @Test
    fun `처리 시각이 오면 동의 안내에 기기 시간대로 붙인다`() {
        assertEquals(
            "마케팅 정보 수신에 동의했어요 · 2026.09.14 20:58 처리",
            consentMessage(agreed = true, syncedAt = "2026-09-14T11:58:08.548315148Z", zone = seoul),
        )
    }

    @Test
    fun `철회도 처리 시각을 함께 알린다`() {
        assertEquals(
            "마케팅 정보 수신을 철회했어요 · 2026.09.14 20:58 처리",
            consentMessage(agreed = false, syncedAt = "2026-09-14T11:58:01Z", zone = seoul),
        )
    }

    @Test
    fun `처리 시각을 모르면 시각을 지어내지 않고 안내만 띄운다`() {
        assertEquals("마케팅 정보 수신에 동의했어요", consentMessage(agreed = true, syncedAt = null, zone = seoul))
    }
}
