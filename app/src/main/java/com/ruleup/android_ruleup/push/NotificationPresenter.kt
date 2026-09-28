package com.ruleup.android_ruleup.push

import android.Manifest
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
import androidx.core.net.toUri
import com.ruleup.android_ruleup.MainActivity
import com.ruleup.android_ruleup.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 포그라운드 푸시를 트레이에 올린다. */
@Singleton
class NotificationPresenter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun show(push: PushMessage) {
            ensureChannel()
            // 권한이 없으면 조용히 지나간다
            if (!canPost()) return

            val notification =
                NotificationCompat
                    .Builder(context, DEFAULT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_ruleup)
                    .setContentTitle(push.title)
                    .setContentText(push.body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(push.body))
                    .setAutoCancel(true)
                    // Doze 유예를 피한다
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(contentIntent(push))
                    .build()

            // canPost() 가 앞에서 권한을 확인했지만, 그 사이 사용자가 회수할 수 있어 예외를 삼킨다.
            @Suppress("MissingPermission")
            runCatching {
                NotificationManagerCompat
                    .from(context)
                    // tag 가 곧 중복 방어다.
                    .notify(push.notificationId, FIXED_ID, notification)
            }
        }

        /** 탭하면 열릴 인텐트. */
        private fun contentIntent(push: PushMessage): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    push.deeplink?.let { data = it.toUri() }
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            return PendingIntent.getActivity(
                context,
                // 알림마다 다른 requestCode 여야 앞선 PendingIntent 의 extras 를 재사용하지 않는다.
                push.notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /** 알림을 올릴 수 있는 상태인가. */
        private fun canPost(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        /** 채널은 지금 하나다. */
        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(DEFAULT_CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(DEFAULT_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH),
            )
        }

        companion object {
            /** 매니페스트의 `default_notification_channel_id` 와 같아야 한다. */
            const val DEFAULT_CHANNEL_ID = "ruleup_default"
            private const val CHANNEL_NAME = "룰업 알림"

            // tag 로 갈리므로 숫자 id 는 고정이어도 서로 덮어쓰지 않는다.
            private const val FIXED_ID = 1
        }
    }
