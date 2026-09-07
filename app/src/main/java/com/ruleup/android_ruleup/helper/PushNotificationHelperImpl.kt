package com.ruleup.android_ruleup.helper

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ruleup.android_ruleup.MainActivity
import com.ruleup.android_ruleup.deeplink.toAppLinkUri
import com.ruleup.domain.helper.PushNotificationHelper
import com.ruleup.domain.navigation.NavRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [PushNotificationHelper] 안드로이드 구현. 탭하면 **[MainActivity] 를 명시한 인텐트**로 진입한다.
 *
 * 암시적 `ACTION_VIEW` 로 두면 매니페스트에 `pathPrefix="/app"` 필터가 필요하고, 그 순간
 * 아무 웹페이지나 같은 URL 로 앱 화면을 열 수 있게 된다(#179).
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
            route: NavRoute,
        ) {
            if (!canPostNotifications()) return
            ensureNotificationChannel()
            val notification =
                NotificationCompat
                    .Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setContentIntent(destinationPendingIntent(id, route))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
            NotificationManagerCompat.from(context).notify(id, notification)
        }

        private fun destinationPendingIntent(
            id: Int,
            route: NavRoute,
        ): PendingIntent {
            // 목적지는 data URI 로 싣되 인텐트는 MainActivity 를 명시한다 — 매니페스트에 /app 필터가
            // 없으므로(#179) 이 URI 는 웹에서 열 수 없고, 진입 해석은 URI 한 갈래로 모인다.
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    data = route.toAppLinkUri()
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
