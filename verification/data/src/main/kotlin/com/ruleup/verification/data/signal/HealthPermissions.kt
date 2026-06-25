package com.ruleup.verification.data.signal

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

/**
 * Health Connect 권한·가용성 헬퍼(명세 §4 셋업·§8). 모든 진입점은 API 26+ 가드 뒤에서만 호출된다
 * (connect-client minSdk 26 + java.time 사용). 미지원/미설치 기기는 HC unavailable 로 떨어져
 * 수동 폴백 트리거(§9.2)가 된다.
 */
@RequiresApi(Build.VERSION_CODES.O)
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
}
