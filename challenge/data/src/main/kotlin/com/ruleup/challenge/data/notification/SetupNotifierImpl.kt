package com.ruleup.challenge.data.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.repository.SetupNotifier
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.domain.helper.PushNotificationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** [SetupNotifier] 구현. */
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
            REGISTER_ANCHOR("인증 장소 등록이 필요해요", "'%s' 인증에 사용할 장소를 등록해주세요"),
            REGISTER_APPS("대상 앱 등록이 필요해요", "'%s' 인증에 사용할 앱을 등록해주세요"),
        }

        override fun notifyAfterCreate(
            challengeId: String,
            title: String,
            verification: VerificationConfig,
            personalSetupRequired: Boolean,
        ) {
            // 수동 인증은 셋업이 없다.
            if (!verification.type.isAuto || !personalSetupRequired) return

            val kind = kindFor(challengeId, verification) ?: return
            pushNotificationHelper.show(
                id = challengeId.hashCode(),
                title = kind.title,
                message = kind.bodyFormat.format(title),
                route = NavRoute(AppRoutes.CHALLENGE_DETAIL, mapOf("challengeId" to challengeId)),
            )
        }

        /** 권한이 우선이다 */
        private fun kindFor(
            challengeId: String,
            verification: VerificationConfig,
        ): Kind? {
            if (!hasPermissions(verification.requiredPermissions)) return Kind.PERMISSION
            return when (verification.method) {
                // 앵커 바인딩 여부는 서버만 알아서(anchorsConfigured) 여기서는 확인하지 않는다.
                VerificationMethod.GPS_PRESENCE, VerificationMethod.GPS_AVOID -> Kind.REGISTER_ANCHOR
                // 사용 시간은 상한·하한 어느 쪽이든 어떤 앱을 볼지 골라야 한다.
                VerificationMethod.SCREEN_TIME_MAX, VerificationMethod.SCREEN_TIME_MIN ->
                    Kind.REGISTER_APPS.takeIf { !targetAppStore.isRegistered(challengeId) }
                // 걸음·기상·취침은 권한만 있으면 되고, 수동은 위에서 이미 걸러졌다.
                VerificationMethod.HEALTH,
                VerificationMethod.WAKE,
                VerificationMethod.SLEEP,
                VerificationMethod.SELF_CHECK,
                -> null
            }
        }

        // 토큰 → OS 런타임 권한 확인.
        private fun hasPermissions(tokens: List<String>): Boolean =
            tokens.all { token ->
                val permission = androidPermission(token) ?: return@all true
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }

        private fun androidPermission(token: String): String? =
            when (token.uppercase()) {
                "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> Manifest.permission.ACCESS_FINE_LOCATION
                "CAMERA", "PHOTO" -> Manifest.permission.CAMERA
                else -> null
            }
    }
