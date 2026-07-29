package com.ruleup.challenge.data.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.ruleup.challenge.domain.repository.SetupNotifier
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.domain.helper.PushNotificationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SetupNotifier] 구현. "무슨 알림을 띄울지"만 결정하고(권한 미허용 → 앱 미등록), 실제 시스템 알림 발송은
 * [PushNotificationHelper] 에 위임한다.
 * 탭 시 상세 화면으로 진입해 남은 셋업(권한 허용/앱 등록)을 이어가게 한다.
 */
@Singleton
class SetupNotifierImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val targetAppStore: TargetAppStore,
        private val pushNotificationHelper: PushNotificationHelper,
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
                    !hasPermissions(requiredPermissions) -> Kind.PERMISSION
                    !targetAppStore.isRegistered(challengeId) -> Kind.REGISTER_APPS
                    else -> return // 이미 셋업 완료
                }
            pushNotificationHelper.show(
                id = challengeId.hashCode(),
                title = kind.title,
                message = kind.bodyFormat.format(title),
                route = NavRoute(AppRoutes.CHALLENGE_DETAIL, mapOf("challengeId" to challengeId)),
            )
        }

        // 토큰 → OS 런타임 권한 확인. 매핑 안 되는 특수권한(usage/health 등)은 런타임 권한이 아니라 허용으로 간주.
        private fun hasPermissions(tokens: List<String>): Boolean =
            tokens.all { token ->
                val permission = androidPermission(token) ?: return@all true
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }

        private fun androidPermission(token: String): String? =
            when (token.uppercase()) {
                "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> Manifest.permission.ACCESS_FINE_LOCATION
                "ACTIVITY_RECOGNITION", "PHYSICAL_ACTIVITY" -> Manifest.permission.ACTIVITY_RECOGNITION
                "CAMERA", "PHOTO" -> Manifest.permission.CAMERA
                else -> null
            }

        private companion object {
        }
    }
