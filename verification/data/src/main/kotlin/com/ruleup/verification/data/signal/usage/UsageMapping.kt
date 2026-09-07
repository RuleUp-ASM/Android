package com.ruleup.verification.data.signal.usage

import android.app.usage.UsageEvents
import com.ruleup.verification.data.db.usage.UsageEventKind
import com.ruleup.verification.data.db.usage.UsageEventType

/** UsageStats 이벤트 코드 → 버퍼 (kind, eventType). (순수, 테스트 가능) */
internal data class UsageMapping(
    val kind: UsageEventKind,
    val eventType: UsageEventType,
)

/**
 * 수집 대상 이벤트만 매핑하고 나머지는 null 로 버린다(명세 §2.2).
 * - 사용량/시간창: ACTIVITY_RESUMED(1)/PAUSED(2)/STOPPED(23) — 대상 패키지 필터는 수집기에서.
 * - WAKE: KEYGUARD_HIDDEN(18) 1순위, SCREEN_INTERACTIVE(15) 폴백.
 */
internal fun usageMappingOf(eventType: Int): UsageMapping? =
    when (eventType) {
        UsageEvents.Event.ACTIVITY_RESUMED -> UsageMapping(UsageEventKind.APP, UsageEventType.RESUMED)
        UsageEvents.Event.ACTIVITY_PAUSED -> UsageMapping(UsageEventKind.APP, UsageEventType.PAUSED)
        UsageEvents.Event.ACTIVITY_STOPPED -> UsageMapping(UsageEventKind.APP, UsageEventType.STOPPED)
        UsageEvents.Event.KEYGUARD_HIDDEN -> UsageMapping(UsageEventKind.SCREEN, UsageEventType.UNLOCK)
        UsageEvents.Event.SCREEN_INTERACTIVE -> UsageMapping(UsageEventKind.SCREEN, UsageEventType.SCREEN_ON)
        else -> null
    }
