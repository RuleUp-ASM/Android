package com.ruleup.verification.data.db

import com.ruleup.verification.data.db.common.toAppEvent
import com.ruleup.verification.data.db.usage.KIND_APP
import com.ruleup.verification.data.db.usage.KIND_SCREEN
import com.ruleup.verification.data.db.usage.UsageEventEntity
import com.ruleup.verification.domain.entity.AppEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsageEntityMapperTest {
    @Test
    fun `APP 행은 앱 사용 이벤트로 매핑된다`() {
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
    }

    @Test
    fun `SCREEN 행은 앱 사용 이벤트로 변환되지 않는다`() {
        // 화면·잠금해제는 SCREEN_TIME 에 섞지 않는다 — 당일 첫 시각만 뽑아 WAKE 로 따로 나간다.
        val screen =
            UsageEventEntity(
                kind = KIND_SCREEN,
                packageName = "",
                eventType = "UNLOCK",
                occurredAt = 20L,
            )

        assertNull(screen.toAppEvent())
    }
}
