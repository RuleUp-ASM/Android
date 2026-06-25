package com.ruleup.verification.data.signal

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * 스크린타임/WAKE 특수권한(PACKAGE_USAGE_STATS) 허용 여부. 런타임 권한이 아니라 AppOps 로 확인한다
 * (Settings 딥링크에서 토글 후 돌아왔을 때 실제 허용 재확인, 명세 §4).
 */
internal fun Context.hasUsageAccess(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
    return mode == AppOpsManager.MODE_ALLOWED
}

/** 사용 정보 접근 설정 화면 딥링크(명세 §4). presentation 이 startActivity 로 띄운다(Phase 5). */
fun usageAccessSettingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
