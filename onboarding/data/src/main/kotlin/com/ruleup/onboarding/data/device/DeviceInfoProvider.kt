package com.ruleup.onboarding.data.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.ruleup.onboarding.data.auth.dto.DeviceInfoRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기기 정적 프로필. 로그인·가입 양쪽 요청에 동반해 서버가 최신 1건으로 갱신한다.
 *
 * 예전엔 `AuthRepositoryImpl` 안의 private 확장이었는데, 로그인·가입이 같은 값을 쓰고 앞으로
 * 기기 스펙 기반 추천에도 쓰인다고 해서 꺼냈다.
 */
@Singleton
class DeviceInfoProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun current(): DeviceInfoRequest {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    packageInfo.versionCode
                }
            return DeviceInfoRequest(
                platform = PLATFORM_ANDROID,
                osVersion = Build.VERSION.RELEASE.orEmpty(),
                sdkInt = Build.VERSION.SDK_INT,
                deviceModel = Build.MODEL.orEmpty(),
                manufacturer = Build.MANUFACTURER.orEmpty(),
                lowRam = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false,
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = versionCode,
            )
        }

        companion object {
            const val PLATFORM_ANDROID = "ANDROID"
        }
    }
