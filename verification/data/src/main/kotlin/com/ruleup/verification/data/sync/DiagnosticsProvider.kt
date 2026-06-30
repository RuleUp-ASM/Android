package com.ruleup.verification.data.sync

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.health.connect.client.HealthConnectClient
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.entity.DeviceDiagnostics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * worker heartbeat 진단 채집(전송 스펙 §0.7). 백그라운드 실행 건강성(standby bucket·배터리 제한·
 * HC 가용성)을 매 flush 동봉한다. 전부 best-effort 라 조회 실패/미지원이면 null.
 */
class DiagnosticsProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: VerificationSettingsStore,
    ) {
        suspend fun snapshot(): DeviceDiagnostics =
            DeviceDiagnostics(
                lastSuccessfulFlushAt = settings.lastSuccessfulFlushAt(),
                standbyBucket = standbyBucket(),
                backgroundRestricted = backgroundRestricted(),
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(),
                // 실행 지연 추정(쿼터 강등 정황)은 Worker 가 산정해 채운다 — 여기선 미산정.
                expeditedDeferred = null,
                lastGeofenceReregisterAt = settings.lastGeofenceReregisterAt(),
                hcSdkStatus = hcSdkStatus(),
            )

        private fun standbyBucket(): Int? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
            return runCatching { usm.appStandbyBucket }.getOrNull()
        }

        private fun backgroundRestricted(): Boolean? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            val am = context.getSystemService(ActivityManager::class.java) ?: return null
            return runCatching { am.isBackgroundRestricted }.getOrNull()
        }

        private fun isIgnoringBatteryOptimizations(): Boolean? {
            val pm = context.getSystemService(PowerManager::class.java) ?: return null
            return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrNull()
        }

        private fun hcSdkStatus(): String =
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> "SDK_AVAILABLE"
                HealthConnectClient.SDK_UNAVAILABLE -> "SDK_UNAVAILABLE"
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED"
                else -> "UNKNOWN"
            }
    }
