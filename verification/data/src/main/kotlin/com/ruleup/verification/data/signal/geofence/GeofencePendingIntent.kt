package com.ruleup.verification.data.signal.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

internal const val ACTION_GEOFENCE_EVENT = "com.ruleup.verification.GEOFENCE_EVENT"

/** 지오펜스 전이를 받을 PendingIntent. */
internal fun geofencePendingIntent(context: Context): PendingIntent =
    PendingIntent.getBroadcast(
        context.applicationContext,
        0,
        geofenceIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag(),
    )

internal fun hasGeofencePendingIntent(context: Context): Boolean =
    PendingIntent.getBroadcast(
        context.applicationContext,
        0,
        geofenceIntent(context),
        PendingIntent.FLAG_NO_CREATE or mutableFlag(),
    ) != null

private fun geofenceIntent(context: Context): Intent =
    Intent(context.applicationContext, GeofenceBroadcastReceiver::class.java).setAction(ACTION_GEOFENCE_EVENT)

private fun mutableFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
