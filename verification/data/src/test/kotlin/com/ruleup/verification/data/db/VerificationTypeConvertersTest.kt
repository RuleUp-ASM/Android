package com.ruleup.verification.data.db

import com.ruleup.verification.data.db.common.VerificationTypeConverters
import com.ruleup.verification.data.db.usage.UsageEventKind
import com.ruleup.verification.data.db.usage.UsageEventType
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.RecordingMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class VerificationTypeConvertersTest {
    private val converters = VerificationTypeConverters()

    @Test
    fun `enum 은 name 그대로 TEXT 가 되고 되읽어도 같은 값이다`() {
        // 저장 형식이 name 에서 벗어나면 컬럼 값이 통째로 바뀌어 이미 깔린 DB 의 행이 전부 폴백으로 떨어진다.
        UsageEventKind.entries.forEach {
            assertEquals(it.name, converters.fromUsageEventKind(it))
            assertEquals(it, converters.toUsageEventKind(it.name))
        }
        UsageEventType.entries.forEach {
            assertEquals(it.name, converters.fromUsageEventType(it))
            assertEquals(it, converters.toUsageEventType(it.name))
        }
        HealthMetric.entries.forEach {
            assertEquals(it.name, converters.fromHealthMetric(it))
            assertEquals(it, converters.toHealthMetric(it.name))
        }
        RecordingMethod.entries.forEach {
            assertEquals(it.name, converters.fromRecordingMethod(it))
            assertEquals(it, converters.toRecordingMethod(it.name))
        }
        GapReason.entries.forEach {
            assertEquals(it.name, converters.fromGapReason(it))
            assertEquals(it, converters.toGapReason(it.name))
        }
    }

    @Test
    fun `미인식 값은 예외 대신 폴백으로 떨어진다`() {
        // valueOf 로 되돌리면 구버전으로 내려간 기기에서 행 하나가 sync 드레인 전체를 죽인다.
        assertEquals(UsageEventKind.SCREEN, converters.toUsageEventKind("FUTURE_KIND"))
        assertEquals(UsageEventType.STOPPED, converters.toUsageEventType("FUTURE_EVENT"))
        assertEquals(HealthMetric.STEPS, converters.toHealthMetric("CALORIES"))
        assertEquals(RecordingMethod.UNKNOWN, converters.toRecordingMethod("FUTURE_VALUE"))
        assertEquals(GapReason.BUFFER_EVICTED, converters.toGapReason("FUTURE_REASON"))
    }
}
