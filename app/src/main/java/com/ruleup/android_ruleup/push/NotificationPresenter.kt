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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.ruleup.designsystem.R as DesignSystemR

/**
 * 포그라운드 푸시를 트레이에 올린다.
 *
 * 백그라운드·종료 상태에서는 OS 가 payload 의 `notification` 블록을 자동 표시하므로 이 코드가
 * 돌지 않는다 — 그래서 [DEFAULT_CHANNEL_ID] 를 매니페스트 기본 채널로도 선언해 둔다. 선언이 없으면
 * 서버가 지정한 채널이 없을 때 알림이 조용히 사라진다.
 *
 * **중복 배송 방어는 클라이언트가 전담한다**(테크 스펙 7) — 재시도로 같은 알림이 두 번 와도
 * `notification_id` 를 tag 로 쓰면 트레이에서 덮어써진다.
 */
@Singleton
class NotificationPresenter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun show(push: PushMessage) {
            ensureChannel()
            // 권한이 없으면 조용히 지나간다 — 알림 센터에는 이미 적재돼 있어 사용자가 볼 길이 남는다.
            if (!canPost()) return

            val notification =
                NotificationCompat
                    .Builder(context, DEFAULT_CHANNEL_ID)
                    .setSmallIcon(DesignSystemR.drawable.ic_bell)
                    .setContentTitle(push.title)
                    .setContentText(push.body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(push.body))
                    .setAutoCancel(true)
                    // Doze 유예를 피한다 — 08:00 일괄 발송이 밀리면 그날 알림이 몰려 온다.
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(contentIntent(push))
                    .build()

            // canPost() 가 앞에서 권한을 확인했지만, 그 사이 사용자가 회수할 수 있어 예외를 삼킨다.
            @Suppress("MissingPermission")
            runCatching {
                NotificationManagerCompat
                    .from(context)
                    // tag 가 곧 중복 방어다. id 는 tag 와 함께 쓰이므로 고정값으로 둔다.
                    .notify(push.notificationId, FIXED_ID, notification)
            }
        }

        /**
         * 탭하면 열릴 인텐트.
         *
         * `MainActivity` 를 **명시한** 인텐트에 딥링크를 실어 보낸다 — 매니페스트 필터를 타지 않으므로
         * 웹페이지가 같은 URI 로 앱 화면을 여는 경로가 생기지 않는다(#179 와 같은 규칙).
         */
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

        /**
         * 알림을 올릴 수 있는 상태인가.
         *
         * 33+ 는 런타임 권한이 따로 있고, 그 아래는 사용자가 앱 알림 자체를 끌 수 있다 — 둘 다 봐야
         * "권한은 있는데 앱 알림이 꺼진" 경우를 놓치지 않는다.
         */
        private fun canPost(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        /**
         * 채널은 지금 하나다.
         *
         * 그룹 토글과 1:1(4채널)로 나눌지는 미정이고(테크 스펙 오픈 이슈 #4), 나누면 **OS 에서 끈
         * 채널을 앱 설정 화면에 반영**해야 한다 — 그 규칙이 정해지기 전에 채널을 쪼개면 앱 설정과
         * OS 설정이 서로 다른 말을 하게 된다.
         */
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
