package com.ruleup.verification.data.sync

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.ruleup.verification.data.signal.PermissionSnapshotProvider
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.DeviceProfile
import com.ruleup.verification.domain.repository.DeviceIntroProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Phase 0 인트로 페이로드 채집(전송 스펙 §0.3). 정적 프로필(sdk/모델/lowRam/앱버전) + 최초 권한 스냅샷.
 */
class DeviceIntroProviderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val permissionSnapshotProvider: PermissionSnapshotProvider,
    ) : DeviceIntroProvider {
        override suspend fun capture(): DeviceIntro {
            val activityManager = context.getSystemService(ActivityManager::class.java)
            val lowRam = activityManager?.isLowRamDevice ?: false
            val profile =
                DeviceProfile(
                    sdkInt = Build.VERSION.SDK_INT,
                    model = Build.MODEL.orEmpty(),
                    lowRam = lowRam,
                    appVersion = appVersion(),
                )
            return DeviceIntro(profile = profile, permissions = permissionSnapshotProvider.capture())
        }

        private fun appVersion(): String =
            try {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                    .orEmpty()
            } catch (e: Exception) {
                ""
            }
    }
