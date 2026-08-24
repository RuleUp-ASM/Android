package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PermissionSnapshotTest {
    @Test
    fun `사용정보 접근과 헬스 커넥트가 허용 여부로 판정된다`() {
        // 종전에는 이 둘이 런타임 권한이 아니라는 이유로 "요청 불가 → 허용"으로 통과했고,
        // 권한 없이 참여가 성립해 매일 NO_SIGNAL_RECEIVED 로 실패했다.
        val denied = snapshot(usageStats = PermissionState.DENIED, healthSteps = PermissionState.DENIED)

        assertFalse(denied.isGranted("PACKAGE_USAGE_STATS")!!)
        assertFalse(denied.isGranted("READ_STEPS")!!)
    }

    @Test
    fun `허용된 권한은 통과한다`() {
        val granted = snapshot()

        assertTrue(granted.isGranted("ACCESS_FINE_LOCATION")!!)
        assertTrue(granted.isGranted("PACKAGE_USAGE_STATS")!!)
    }

    @Test
    fun `모르는 토큰은 판단을 보류한다`() {
        // 서버가 토큰을 추가했을 때 그 하나 때문에 참여를 막으면 구버전 앱이 통째로 잠긴다.
        assertNull(snapshot().isGranted("FUTURE_PERMISSION"))
    }

    @Test
    fun `설정에서만 켤 수 있는 권한이 구분된다`() {
        // OS 다이얼로그가 없는 권한을 "허용하기" 버튼으로 안내하면 눌러도 아무 일이 안 일어난다.
        assertTrue(PermissionSnapshot.requiresSettings("PACKAGE_USAGE_STATS"))
        assertTrue(PermissionSnapshot.requiresSettings("READ_SLEEP"))
        assertFalse(PermissionSnapshot.requiresSettings("ACCESS_FINE_LOCATION"))
        assertFalse(PermissionSnapshot.requiresSettings("POST_NOTIFICATIONS"))
    }

    @Test
    fun `대소문자가 달라도 같은 토큰으로 읽는다`() {
        assertEquals(snapshot().isGranted("access_fine_location"), snapshot().isGranted("ACCESS_FINE_LOCATION"))
    }

    private fun snapshot(
        usageStats: PermissionState = PermissionState.GRANTED,
        healthSteps: PermissionState = PermissionState.GRANTED,
    ): PermissionSnapshot =
        PermissionSnapshot(
            location = PermissionState.GRANTED,
            backgroundLocation = PermissionState.GRANTED,
            activityRecognition = PermissionState.GRANTED,
            usageStats = usageStats,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = healthSteps,
            healthSleep = PermissionState.GRANTED,
            healthBackground = PermissionState.GRANTED,
        )
}
