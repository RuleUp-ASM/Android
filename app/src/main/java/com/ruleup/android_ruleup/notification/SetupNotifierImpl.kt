package com.ruleup.android_ruleup.notification

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
import com.ruleup.android_ruleup.MainActivity
import com.ruleup.challenge.domain.SetupNotifier
import com.ruleup.challenge.domain.TargetAppStore
import com.ruleup.challenge.presentation.create.component.challengePermissionsGranted
import com.ruleup.domain.navigation.AppRoutes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SetupNotifier] 안드로이드 구현. 생성 직후 셋업 미완료면 로컬 알림을 띄우고, 탭 시 딥링크로 상세 화면에
 * 진입시킨다(상세가 상태를 보고 "권한 허용하기"/"앱 등록하기" 버튼을 자동으로 정한다).
 */
@Singleton
class SetupNotifierImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val targetAppStore: TargetAppStore,
    ) : SetupNotifier {
        private enum class Kind(
            val title: String,
            val bodyFormat: String,
        ) {
            PERMISSION("권한 허용이 필요해요", "'%s' 자동 인증을 위해 권한을 허용해주세요"),
            REGISTER_APPS("대상 앱 등록이 필요해요", "'%s' 인증에 사용할 앱을 등록해주세요"),
        }

        override fun notifyAfterCreate(
            challengeId: String,
            title: String,
            requiredPermissions: List<String>,
            isAuto: Boolean,
        ) {
            // 수동 인증은 셋업이 없어 알림 없음.
            if (!isAuto) return
            val kind =
                when {
                    !challengePermissionsGranted(context, requiredPermissions) -> Kind.PERMISSION
                    !targetAppStore.isRegistered(challengeId) -> Kind.REGISTER_APPS
                    else -> return // 이미 셋업 완료
                }
            post(challengeId, title, kind)
        }

        @SuppressLint("MissingPermission") // 33+ 는 아래에서 POST_NOTIFICATIONS 를 직접 확인한 뒤에만 notify 한다.
        private fun post(
            challengeId: String,
            title: String,
            kind: Kind,
        ) {
            if (!canPostNotifications()) return
            ensureChannel()

            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(kind.title)
                    .setContentText(kind.bodyFormat.format(title))
                    .setContentIntent(detailPendingIntent(challengeId))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            NotificationManagerCompat.from(context).notify(challengeId.hashCode(), notification)
        }

        // 탭 → MainActivity 로 딥링크(ruleup://app/challenge/detail?challengeId=...). 실행 중이면 onNewIntent,
        // 콜드 스타트면 onCreate 의 시작 스택 구성으로 상세에 진입한다.
        private fun detailPendingIntent(challengeId: String): PendingIntent {
            val uri = Uri.parse("$DEEP_LINK_PREFIX/${AppRoutes.CHALLENGE_DETAIL}?challengeId=$challengeId")
            val intent =
                Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            return PendingIntent.getActivity(
                context,
                challengeId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "챌린지 셋업 안내", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        private fun canPostNotifications(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        companion object {
            private const val CHANNEL_ID = "challenge_setup"
            private const val DEEP_LINK_PREFIX = "ruleup://app"
        }
    }
