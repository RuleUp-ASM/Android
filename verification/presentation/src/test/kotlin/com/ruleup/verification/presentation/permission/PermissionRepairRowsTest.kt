package com.ruleup.verification.presentation.permission

import com.ruleup.verification.domain.entity.PermissionRequestKind
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionRepairRowsTest {
    @Test
    fun `살아 있는 신호도 함께 나열한다`() {
        // 끊긴 것만 보여주면 "무엇은 멀쩡한지"를 알 수 없어 원인을 좁히지 못한다.
        val rows = repairRows(snapshot(location = PermissionState.DENIED))

        assertTrue(rows.any { it.label == "위치" && !it.granted })
        assertTrue(rows.any { it.granted })
    }

    @Test
    fun `권한마다 여는 문이 다르다`() {
        val rows = repairRows(snapshot()).associateBy { it.label }

        assertEquals(PermissionRequestKind.USAGE_ACCESS_SETTINGS, rows.getValue("사용 정보 접근").kind)
        assertEquals(PermissionRequestKind.HEALTH_CONNECT, rows.getValue("걸음").kind)
        assertEquals(PermissionRequestKind.HEALTH_CONNECT, rows.getValue("수면").kind)
        assertEquals(PermissionRequestKind.RUNTIME, rows.getValue("위치").kind)
    }

    @Test
    fun `런타임 권한만 요청할 권한 목록을 갖는다`() {
        val rows = repairRows(snapshot()).associateBy { it.label }

        assertTrue(rows.getValue("위치").runtimePermissions.isNotEmpty())
        assertTrue(rows.getValue("사용 정보 접근").runtimePermissions.isEmpty())
        assertTrue(rows.getValue("걸음").runtimePermissions.isEmpty())
    }

    @Test
    fun `백그라운드 건강 데이터가 꺼져 있으면 헬스 커넥트로 고치는 줄을 세운다`() {
        // 이 줄이 없으면 앱이 꺼진 동안 걸음·수면이 안 모이는데 사용자가 고칠 곳이 없다(#567).
        val row =
            repairRows(snapshot(healthBackground = PermissionState.DENIED))
                .single { it.label == "백그라운드 건강 데이터" }

        assertFalse(row.granted)
        assertEquals(PermissionRequestKind.HEALTH_CONNECT, row.kind)
    }

    @Test
    fun `백그라운드 읽기를 지원하지 않는 기기에는 고칠 수 없는 줄을 세우지 않는다`() {
        // 허용할 방법이 없는데 끊김으로 보이면 사용자는 끝없이 고치려 든다. 앱을 열었을 때 모은다.
        val rows = repairRows(snapshot(healthBackground = PermissionState.DENIED, backgroundSupported = false))

        assertTrue(rows.none { it.label == "백그라운드 건강 데이터" })
    }

    private fun snapshot(
        location: PermissionState = PermissionState.GRANTED,
        healthBackground: PermissionState = PermissionState.GRANTED,
        backgroundSupported: Boolean = true,
    ): PermissionSnapshot =
        PermissionSnapshot(
            location = location,
            backgroundLocation = PermissionState.GRANTED,
            usageStats = PermissionState.GRANTED,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = PermissionState.GRANTED,
            healthSleep = PermissionState.GRANTED,
            healthBackground = healthBackground,
            healthBackgroundSupported = backgroundSupported,
        )
}
