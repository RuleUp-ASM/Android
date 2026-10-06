package com.ruleup.verification.data.signal.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

/** Health Connect 권한·가용성 헬퍼. */
object HealthPermissions {
    /** 움직임·수면 읽기 권한 집합. */
    fun readPermissions(): Set<String> =
        setOf(
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
        )

    /** Health Connect 앱이 설치·사용 가능할 때만 클라이언트를 반환(아니면 null → 수집 생략). */
    fun clientOrNull(context: Context): HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }

    /** 앱이 꺼진 동안의 읽기를 지원하는 기기인가. */
    fun backgroundReadAvailable(context: Context): Boolean =
        clientOrNull(context)
            ?.features
            ?.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

    /** Health Connect 사용 가능 여부(설치·지원). */
    fun isAvailable(context: Context): Boolean = clientOrNull(context) != null

    /** HC 권한 요청 컨트랙트(입력=요청 권한, 출력=허용된 권한). */
    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()
}
