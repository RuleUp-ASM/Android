package com.ruleup.verification.data.db

import com.ruleup.verification.data.db.common.toAppEvent
import com.ruleup.verification.data.db.usage.UsageEventEntity
import com.ruleup.verification.data.db.usage.UsageEventKind
import com.ruleup.verification.data.db.usage.UsageEventType
import com.ruleup.verification.domain.entity.AppEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsageEntityMapperTest {
    @Test
    fun `APP 행은 앱 사용 이벤트로 매핑된다`() {
        val app =
            UsageEventEntity(
                kind = UsageEventKind.APP,
                packageName = "com.example.shop",
                eventType = UsageEventType.RESUMED,
                occurredAt = 10L,
            )

        val appEvent = app.toAppEvent()
        assertEquals("com.example.shop", appEvent?.packageName)
        assertEquals(AppEventType.RESUMED, appEvent?.eventType)
        assertEquals(10L, appEvent?.at)
    }

    @Test
    fun `SCREEN 행은 앱 사용 이벤트로 변환되지 않는다`() {
        // 화면·잠금해제는 SCREEN_TIME 에 섞지 않는다 — 당일 첫 시각만 뽑아 WAKE 로 따로 나간다.
        val screen =
            UsageEventEntity(
                kind = UsageEventKind.SCREEN,
                packageName = "",
                eventType = UsageEventType.UNLOCK,
                occurredAt = 20L,
            )

        assertNull(screen.toAppEvent())
    }

    @Test
    fun `APP 행이어도 화면 이벤트면 AppEventType 을 지어내지 않는다`() {
        // 예전 폴백(`?: RESUMED`)이 살아나면 잠금해제 한 번이 앱 실행으로 둔갑해 사용 시간에 섞인다.
        val broken =
            UsageEventEntity(
                kind = UsageEventKind.APP,
                packageName = "com.example.shop",
                eventType = UsageEventType.UNLOCK,
                occurredAt = 30L,
            )

        assertNull(broken.toAppEvent())
    }
}
