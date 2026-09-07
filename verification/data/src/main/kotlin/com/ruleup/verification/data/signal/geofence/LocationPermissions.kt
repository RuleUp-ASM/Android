package com.ruleup.verification.data.signal.geofence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build

/** FINE 위치 허용 여부(런타임). 미허용 시 지오펜스 등록·측위 불가 → PERMISSION_MISSING. */
internal fun Context.hasFineLocation(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** 백그라운드 위치 허용 여부(Android 10+). 미허용 시 앱 종료 중 지오펜스 미발화. */
internal fun Context.hasBackgroundLocation(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

/** mock 위치 판정(명세 §7). API31+ isMock / 이하 isFromMockProvider. */
@Suppress("DEPRECATION")
internal fun Location.isMockCompat(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider
