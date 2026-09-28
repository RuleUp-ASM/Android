package com.ruleup.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.ruleup.domain.device.DeviceInfo
import com.ruleup.domain.device.DeviceInfoProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Android 기기 정보 조회. */
class AndroidDeviceInfoProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DeviceInfoProvider {
        override fun current(): DeviceInfo {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    packageInfo.versionCode
                }
            return DeviceInfo(
                platform = "ANDROID",
                osVersion = Build.VERSION.RELEASE.orEmpty(),
                sdkInt = Build.VERSION.SDK_INT,
                deviceModel = Build.MODEL.orEmpty(),
                manufacturer = Build.MANUFACTURER.orEmpty(),
                lowRam = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false,
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = versionCode,
            )
        }
    }
