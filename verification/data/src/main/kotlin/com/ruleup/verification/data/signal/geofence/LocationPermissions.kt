package com.ruleup.verification.data.signal.geofence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build

/** FINE 위치 허용 여부(런타임). */
internal fun Context.hasFineLocation(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** 백그라운드 위치 허용 여부(Android 10+). */
internal fun Context.hasBackgroundLocation(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

/** mock 위치 판정. */
@Suppress("DEPRECATION")
internal fun Location.isMockCompat(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider
