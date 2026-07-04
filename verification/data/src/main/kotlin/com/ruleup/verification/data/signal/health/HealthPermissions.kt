package com.ruleup.verification.data.signal.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

/**
 * Health Connect 권한·가용성 헬퍼(명세 §4 셋업·§8). 미지원/미설치 기기는 HC unavailable 로 떨어져
 * 수동 폴백 트리거(§9.2)가 된다.
 */
object HealthPermissions {
    /** 움직임·수면 읽기 권한 집합(셋업 grant 요청 — ActivityResultContract 입력, 명세 §4). */
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

    /** Health Connect 사용 가능 여부(설치·지원). 권한 요청 전에 확인(미지원이면 요청 자체를 건너뛴다). */
    fun isAvailable(context: Context): Boolean = clientOrNull(context) != null

    /**
     * HC 권한 요청 컨트랙트(입력=요청 권한, 출력=허용된 권한). 호출측은 HC 클래스를 몰라도 Set<String> 로 다룬다
     * — 디버그 권한 트리거가 app 모듈에서 HC 의존 없이 권한 요청을 띄우게 한다.
     */
    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()
}
