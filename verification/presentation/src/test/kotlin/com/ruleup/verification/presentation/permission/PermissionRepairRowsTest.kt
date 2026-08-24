package com.ruleup.verification.presentation.permission

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
    fun `사용 정보 접근은 설정으로 보낸다`() {
        // 런타임 다이얼로그가 없는 권한이라 "허용" 버튼으로는 아무 일도 일어나지 않는다.
        val row = repairRows(snapshot()).single { it.label == "사용 정보 접근" }

        assertTrue(row.settingsIntent != null)
        assertTrue(row.runtimePermissions.isEmpty())
    }

    @Test
    fun `위치는 그 자리에서 다시 요청한다`() {
        val row = repairRows(snapshot()).single { it.label == "위치" }

        assertEquals(null, row.settingsIntent)
        assertTrue(row.runtimePermissions.isNotEmpty())
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
