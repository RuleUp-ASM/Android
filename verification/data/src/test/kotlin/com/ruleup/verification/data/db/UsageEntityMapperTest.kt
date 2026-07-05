package com.ruleup.verification.data.db

import com.ruleup.verification.data.db.common.toAppEvent
import com.ruleup.verification.data.db.common.toScreenEvent
import com.ruleup.verification.data.db.usage.KIND_APP
import com.ruleup.verification.data.db.usage.KIND_SCREEN
import com.ruleup.verification.data.db.usage.UsageEventEntity
import com.ruleup.verification.domain.entity.AppEventType
import com.ruleup.verification.domain.entity.ScreenEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsageEntityMapperTest {
    @Test
    fun `APP 행은 앱 사용 이벤트로 매핑되고 화면 이벤트로는 변환되지 않는다`() {
        val app =
            UsageEventEntity(
                kind = KIND_APP,
                packageName = "com.example.shop",
                eventType = "RESUMED",
                occurredAt = 10L,
            )

        val appEvent = app.toAppEvent()
        assertEquals("com.example.shop", appEvent?.packageName)
        assertEquals(AppEventType.RESUMED, appEvent?.eventType)
        assertEquals(10L, appEvent?.at)
        assertNull(app.toScreenEvent())
    }

    @Test
    fun `SCREEN 행은 화면 이벤트로 매핑되고 앱 이벤트로는 변환되지 않는다`() {
        val screen =
            UsageEventEntity(
                kind = KIND_SCREEN,
                packageName = "",
                eventType = "UNLOCK",
                occurredAt = 20L,
            )

        val screenEvent = screen.toScreenEvent()
        assertEquals(ScreenEventType.UNLOCK, screenEvent?.event)
        assertEquals(20L, screenEvent?.at)
        assertNull(screen.toAppEvent())
    }
}
