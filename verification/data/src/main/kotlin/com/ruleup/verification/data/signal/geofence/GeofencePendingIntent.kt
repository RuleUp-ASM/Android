package com.ruleup.verification.data.signal.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

internal const val ACTION_GEOFENCE_EVENT = "com.ruleup.verification.GEOFENCE_EVENT"

/**
 * 지오펜스 전이를 받을 PendingIntent. Android 12+ 는 시스템이 이벤트 extras 를 채우므로
 * FLAG_MUTABLE 이 필수다.
 */
internal fun geofencePendingIntent(context: Context): PendingIntent {
    val intent =
        Intent(context.applicationContext, GeofenceBroadcastReceiver::class.java)
            .setAction(ACTION_GEOFENCE_EVENT)
    val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    return PendingIntent.getBroadcast(
        context.applicationContext,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or mutable,
    )
}
