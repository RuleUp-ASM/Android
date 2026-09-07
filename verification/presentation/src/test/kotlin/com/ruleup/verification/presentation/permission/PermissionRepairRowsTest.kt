package com.ruleup.verification.presentation.permission

import com.ruleup.verification.domain.entity.PermissionRequestKind
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import kotlin.test.Test
import kotlin.test.assertEquals
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
        // 걸음·수면을 사용정보 접근 설정으로 보내면 거기서 아무리 켜도 그 권한은 생기지 않는다.
        val rows = repairRows(snapshot()).associateBy { it.label }

        assertEquals(PermissionRequestKind.USAGE_ACCESS_SETTINGS, rows.getValue("사용 정보 접근").kind)
        assertEquals(PermissionRequestKind.HEALTH_CONNECT, rows.getValue("걸음·거리").kind)
        assertEquals(PermissionRequestKind.HEALTH_CONNECT, rows.getValue("수면").kind)
        assertEquals(PermissionRequestKind.RUNTIME, rows.getValue("위치").kind)
    }

    @Test
    fun `런타임 권한만 요청할 권한 목록을 갖는다`() {
        val rows = repairRows(snapshot()).associateBy { it.label }

        assertTrue(rows.getValue("위치").runtimePermissions.isNotEmpty())
        assertTrue(rows.getValue("사용 정보 접근").runtimePermissions.isEmpty())
        assertTrue(rows.getValue("걸음·거리").runtimePermissions.isEmpty())
    }

    private fun snapshot(location: PermissionState = PermissionState.GRANTED): PermissionSnapshot =
        PermissionSnapshot(
            location = location,
            backgroundLocation = PermissionState.GRANTED,
            activityRecognition = PermissionState.GRANTED,
            usageStats = PermissionState.GRANTED,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = PermissionState.GRANTED,
            healthSleep = PermissionState.GRANTED,
            healthBackground = PermissionState.GRANTED,
        )
}
