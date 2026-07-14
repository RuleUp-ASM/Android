package com.ruleup.verification.presentation.render

import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.TodayStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerificationCopyTest {
    @Test
    fun `PENDING 과 NOT_TARGET 은 실패 상태가 아니다`() {
        assertFalse(isFailureState(TodayStatus.PENDING))
        assertFalse(isFailureState(TodayStatus.NOT_TARGET))
        assertFalse(isFailureState(TodayStatus.SUCCESS))
        assertTrue(isFailureState(TodayStatus.FAILED))
    }

    @Test
    fun `PENDING 라벨은 진행 중 - 실패 카피가 아니다`() {
        assertEquals("진행 중", todayStatusLabel(TodayStatus.PENDING))
    }

    @Test
    fun `권한 누락은 권한 설정 CTA 로 라우팅된다`() {
        assertEquals(CtaTarget.OPEN_PERMISSION_SETTINGS, failureReasonCta(FailureReason.PERMISSION_MISSING))
        assertEquals(CtaTarget.OPEN_PERMISSION_SETTINGS, failureReasonCta(FailureReason.NO_SIGNAL_RECEIVED))
        assertEquals("권한 설정으로", ctaLabel(failureReasonCta(FailureReason.PERMISSION_MISSING)))
    }

    @Test
    fun `장소 미설정은 지도 핀 CTA 로 라우팅된다`() {
        assertEquals(CtaTarget.OPEN_LOCATION_PIN, failureReasonCta(FailureReason.GEOFENCE_NOT_CONFIGURED))
        assertEquals("지도 핀 설정으로", ctaLabel(failureReasonCta(FailureReason.GEOFENCE_NOT_CONFIGURED)))
    }

    @Test
    fun `체류 부족 등은 CTA 가 없다`() {
        assertEquals(CtaTarget.NONE, failureReasonCta(FailureReason.INSUFFICIENT_DWELL))
        assertNull(ctaLabel(failureReasonCta(FailureReason.INSUFFICIENT_DWELL)))
        assertNotNull(failureReasonCopy(FailureReason.INSUFFICIENT_DWELL))
    }

    @Test
    fun `lastSyncedAt 이 null 이거나 2시간 초과면 신호 미수신`() {
        val now = 10L * 60 * 60 * 1000
        assertTrue(isStaleSync(null, now))
        assertTrue(isStaleSync(now - 3L * 60 * 60 * 1000, now))
        assertFalse(isStaleSync(now - 30L * 60 * 1000, now))
    }

    @Test
    fun `ISO 파싱 실패는 null 로 떨어진다`() {
        assertNull(parseIsoToEpochMillis("not-a-date"))
        assertNull(parseIsoToEpochMillis(null))
        assertNotNull(parseIsoToEpochMillis("2026-06-21T00:00:00Z"))
    }
}
