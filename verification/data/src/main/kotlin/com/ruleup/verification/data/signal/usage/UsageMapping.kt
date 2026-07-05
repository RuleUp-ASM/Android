package com.ruleup.verification.data.signal.usage

import android.app.usage.UsageEvents
import com.ruleup.verification.data.db.usage.KIND_APP
import com.ruleup.verification.data.db.usage.KIND_SCREEN
import com.ruleup.verification.domain.entity.AppEventType
import com.ruleup.verification.domain.entity.ScreenEventType

/** UsageStats 이벤트 코드 → 버퍼 (kind, eventType). (순수, 테스트 가능 — 상수는 컴파일 시 인라인) */
internal data class UsageMapping(
    val kind: String,
    val eventType: String,
)

/**
 * 수집 대상 이벤트만 매핑하고 나머지는 null 로 버린다(명세 §2.2).
 * - 사용량/시간창: ACTIVITY_RESUMED(1)/PAUSED(2)/STOPPED(23) — 대상 패키지 필터는 수집기에서.
 * - WAKE: KEYGUARD_HIDDEN(18) 1순위, SCREEN_INTERACTIVE(15) 폴백.
 */
internal fun usageMappingOf(eventType: Int): UsageMapping? =
    when (eventType) {
        UsageEvents.Event.ACTIVITY_RESUMED -> UsageMapping(KIND_APP, AppEventType.RESUMED.name)
        UsageEvents.Event.ACTIVITY_PAUSED -> UsageMapping(KIND_APP, AppEventType.PAUSED.name)
        UsageEvents.Event.ACTIVITY_STOPPED -> UsageMapping(KIND_APP, AppEventType.STOPPED.name)
        UsageEvents.Event.KEYGUARD_HIDDEN -> UsageMapping(KIND_SCREEN, ScreenEventType.UNLOCK.name)
        UsageEvents.Event.SCREEN_INTERACTIVE -> UsageMapping(KIND_SCREEN, ScreenEventType.SCREEN_ON.name)
        else -> null
    }
