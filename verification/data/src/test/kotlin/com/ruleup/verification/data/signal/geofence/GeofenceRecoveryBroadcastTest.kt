package com.ruleup.verification.data.signal.geofence

import android.content.Intent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeofenceRecoveryBroadcastTest {
    @Test
    fun `재부팅과 Google Play 서비스 데이터 초기화는 지오펜스 복구가 필요하다`() {
        assertTrue(requiresGeofenceRecovery(Intent.ACTION_BOOT_COMPLETED, null))
        assertTrue(requiresGeofenceRecovery(Intent.ACTION_PACKAGE_DATA_CLEARED, "com.google.android.gms"))
    }

    @Test
    fun `다른 앱의 데이터 초기화나 다른 방송은 지오펜스를 재등록하지 않는다`() {
        assertFalse(requiresGeofenceRecovery(Intent.ACTION_PACKAGE_DATA_CLEARED, "com.example.other"))
        assertFalse(requiresGeofenceRecovery(Intent.ACTION_PACKAGE_DATA_CLEARED, null))
        assertFalse(requiresGeofenceRecovery("other.action", "com.google.android.gms"))
        assertFalse(requiresGeofenceRecovery(null, null))
    }
}
