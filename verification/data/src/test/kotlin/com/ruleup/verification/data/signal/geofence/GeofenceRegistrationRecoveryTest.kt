package com.ruleup.verification.data.signal.geofence

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeofenceRegistrationRecoveryTest {
    @Test
    fun `앱이 업데이트된 뒤에는 등록 정보를 다시 확인한다`() {
        assertTrue(needsRegistrationRecovery(100, 101, null, true))
    }

    @Test
    fun `마지막 등록 이후 사용자가 앱을 종료했으면 등록 정보를 다시 확인한다`() {
        assertTrue(needsRegistrationRecovery(100, 90, 101, true))
    }

    @Test
    fun `지오펜스 수신 요청이 사라졌으면 등록 정보를 다시 확인한다`() {
        assertTrue(needsRegistrationRecovery(100, 90, null, false))
    }

    @Test
    fun `과거에 이미 복구한 종료나 일반 프로세스 재시작은 재등록하지 않는다`() {
        assertFalse(needsRegistrationRecovery(100, 90, 99, true))
        assertFalse(needsRegistrationRecovery(100, 90, null, true))
    }

    @Test
    fun `등록 이력이 없으면 기존 등록을 무효화할 필요가 없다`() {
        assertFalse(needsRegistrationRecovery(null, 101, 101, false))
    }
}
