package com.ruleup.android_ruleup.helper

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ruleup.domain.helper.PushNotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [PushNotificationHelper] 안드로이드 구현. NotificationManager 로 시스템 알림을 띄우고,
 * 탭 시 암시적 `ACTION_VIEW`(ruleup://app)로 앱에 진입시킨다(특정 Activity 참조 없음 — 매니페스트 intent-filter 라우팅).
 */
class PushNotificationHelperImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PushNotificationHelper {
        @SuppressLint("MissingPermission") // 33+ 는 canPostNotifications() 로 POST_NOTIFICATIONS 를 확인한 뒤에만 notify.
        override fun show(
            id: Int,
            title: String,
            message: String,
            deepLink: String,
        ) {
            if (!canPostNotifications()) return
            ensureNotificationChannel()
            val notification =
                NotificationCompat
                    .Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setContentIntent(deepLinkPendingIntent(id, deepLink))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
            NotificationManagerCompat.from(context).notify(id, notification)
        }

        private fun deepLinkPendingIntent(
            id: Int,
            deepLink: String,
        ): PendingIntent {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                    setPackage(context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            return PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun ensureNotificationChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, "알림", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        private fun canPostNotifications(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        private companion object {
            const val NOTIFICATION_CHANNEL_ID = "app_default"
        }
    }
