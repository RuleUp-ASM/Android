package com.ruleup.notification.presentation.settings.viewmodel

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 광고성 수신 동의·철회는 **처리된 날**을 알려야 한다.
 *
 * 기준 시간대는 서버(KST)다 — 기기 시간대로 옮기면 해외에서 하루 어긋난 날짜가 보이고, 그 날짜로는
 * 사용자가 수신 이력을 대조할 수 없다. 분 단위는 대조할 방법이 없어 적지 않는다.
 */
class ConsentMessageTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    @Test
    fun `처리된 날이 오면 동의 안내에 서비스 기준 날짜로 붙인다`() {
        assertEquals(
            "마케팅 정보 수신에 동의했어요 · 9월 14일 처리됐어요",
            consentMessage(agreed = true, syncedAt = "2026-09-14T11:58:08.548315148Z", zone = seoul),
        )
    }

    @Test
    fun `철회도 처리된 날을 함께 알린다`() {
        assertEquals(
            "마케팅 정보 수신을 철회했어요 · 9월 14일 처리됐어요",
            consentMessage(agreed = false, syncedAt = "2026-09-14T11:58:01Z", zone = seoul),
        )
    }

    @Test
    fun `처리된 날을 모르면 날짜를 지어내지 않고 안내만 띄운다`() {
        assertEquals("마케팅 정보 수신에 동의했어요", consentMessage(agreed = true, syncedAt = null, zone = seoul))
    }
}
