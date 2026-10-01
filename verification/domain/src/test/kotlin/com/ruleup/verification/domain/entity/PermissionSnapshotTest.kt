package com.ruleup.verification.domain.entity

import com.ruleup.domain.entity.user.AgreementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PermissionSnapshotTest {
    @Test
    fun `전경과 백그라운드 위치 권한은 같은 위치 동의를 요구한다`() {
        listOf("LOCATION", "access_fine_location", "GPS", "GEOFENCE", "ACCESS_BACKGROUND_LOCATION", "BACKGROUND_LOCATION").forEach {
            assertEquals(AgreementType.LOCATION_INFO, PermissionSnapshot.requiredConsentFor(it))
        }
    }

    @Test
    fun `건강 데이터 권한은 종류와 관계없이 건강 동의를 요구한다`() {
        listOf(
            "READ_DISTANCE",
            "HEALTH_DISTANCE",
            "READ_STEPS",
            "HEALTH_STEPS",
            "HEALTH",
            "read_sleep",
            "HEALTH_SLEEP",
            "SLEEP",
            "READ_HEALTH_DATA_IN_BACKGROUND",
            "HEALTH_BACKGROUND",
        ).forEach {
            assertEquals(AgreementType.HEALTH_INFO, PermissionSnapshot.requiredConsentFor(it))
        }
    }

    @Test
    fun `알림과 카메라와 사용 기록 및 미지원 권한은 개별 동의를 추론하지 않는다`() {
        listOf("POST_NOTIFICATIONS", "CAMERA", "PACKAGE_USAGE_STATS", "FUTURE_PERMISSION").forEach {
            assertNull(PermissionSnapshot.requiredConsentFor(it))
        }
    }

    @Test
    fun `사용정보 접근과 헬스 커넥트가 허용 여부로 판정된다`() {
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
        assertNull(snapshot().isGranted("FUTURE_PERMISSION"))
    }

    @Test
    fun `신체 활동 토큰은 더 이상 알아보지 않는다`() {
        assertNull(snapshot().isGranted("ACTIVITY_RECOGNITION"))
        assertNull(snapshot().isGranted("PHYSICAL_ACTIVITY"))
    }

    @Test
    fun `권한마다 여는 문이 다르다`() {
        assertEquals(PermissionRequestKind.USAGE_ACCESS_SETTINGS, PermissionSnapshot.requestKindOf("PACKAGE_USAGE_STATS"))
        assertEquals(PermissionRequestKind.HEALTH_CONNECT, PermissionSnapshot.requestKindOf("READ_SLEEP"))
        assertEquals(PermissionRequestKind.HEALTH_CONNECT, PermissionSnapshot.requestKindOf("READ_STEPS"))
        assertEquals(PermissionRequestKind.RUNTIME, PermissionSnapshot.requestKindOf("ACCESS_FINE_LOCATION"))
        assertEquals(PermissionRequestKind.RUNTIME, PermissionSnapshot.requestKindOf("POST_NOTIFICATIONS"))
    }

    @Test
    fun `대소문자가 달라도 같은 토큰으로 읽는다`() {
        assertEquals(snapshot().isGranted("access_fine_location"), snapshot().isGranted("ACCESS_FINE_LOCATION"))
    }

    @Test
    fun `전체 Android 권한 이름도 허용 상태와 동의 및 요청 경로가 같다`() {
        listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_DISTANCE",
            "android.permission.health.READ_SLEEP",
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND",
        ).forEach { full ->
            val short = full.substringAfterLast('.')
            assertEquals(snapshot().isGranted(short), snapshot().isGranted(full))
            assertEquals(PermissionSnapshot.requiredConsentFor(short), PermissionSnapshot.requiredConsentFor(full))
            assertEquals(PermissionSnapshot.requestKindOf(short), PermissionSnapshot.requestKindOf(full))
        }
    }

    @Test
    fun `위치 인증인데 기기 위치가 꺼져 있으면 알린다`() {
        val off = snapshot().copy(locationServiceEnabled = false)

        assertEquals(true, off.locationServiceOff(listOf("android.permission.ACCESS_FINE_LOCATION")))
        assertEquals(true, off.locationServiceOff(listOf("GEOFENCE")))
    }

    @Test
    fun `위치를 쓰지 않는 챌린지는 기기 위치가 꺼져도 알리지 않는다`() {
        val off = snapshot().copy(locationServiceEnabled = false)

        assertEquals(false, off.locationServiceOff(listOf("READ_STEPS")))
    }

    @Test
    fun `기기 위치가 켜져 있으면 알리지 않는다`() {
        assertEquals(false, snapshot().locationServiceOff(listOf("LOCATION")))
    }

    private fun snapshot(
        usageStats: PermissionState = PermissionState.GRANTED,
        healthSteps: PermissionState = PermissionState.GRANTED,
    ): PermissionSnapshot =
        PermissionSnapshot(
            location = PermissionState.GRANTED,
            backgroundLocation = PermissionState.GRANTED,
            usageStats = usageStats,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = healthSteps,
            healthSleep = PermissionState.GRANTED,
            healthBackground = PermissionState.GRANTED,
        )
}
