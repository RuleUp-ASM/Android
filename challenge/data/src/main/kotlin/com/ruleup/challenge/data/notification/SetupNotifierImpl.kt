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

/**
 * [SetupNotifier] 구현. "무슨 알림을 띄울지"만 결정하고 실제 발송은 [PushNotificationHelper] 에 위임한다.
 * 탭 시 상세 화면으로 진입해 남은 셋업을 이어가게 한다.
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
            REGISTER_ANCHOR("인증 장소 등록이 필요해요", "'%s' 인증에 사용할 장소를 등록해주세요"),
            REGISTER_APPS("대상 앱 등록이 필요해요", "'%s' 인증에 사용할 앱을 등록해주세요"),
        }

        override fun notifyAfterCreate(
            challengeId: String,
            title: String,
            verification: VerificationConfig,
            personalSetupRequired: Boolean,
        ) {
            // 수동 인증은 셋업이 없다. 서버가 설정 불필요라고 했으면 그 판단을 따른다.
            if (!verification.type.isAuto || !personalSetupRequired) return

            val kind = kindFor(challengeId, verification) ?: return
            pushNotificationHelper.show(
                id = challengeId.hashCode(),
                title = kind.title,
                message = kind.bodyFormat.format(title),
                route = NavRoute(AppRoutes.CHALLENGE_DETAIL, mapOf("challengeId" to challengeId)),
            )
        }

        /**
         * 권한이 우선이다 — 권한 없이 등록 화면에 들어가면 아무것도 못 한다.
         *
         * 그 다음은 **인증 방식이 결정한다.** 예전에는 방식을 모른 채 "대상 앱 미등록"만 봐서, 앱을
         * 쓰지 않는 장소형(GPS_PRESENCE) 챌린지에도 앱 등록 알림이 나갔다.
         */
        private fun kindFor(
            challengeId: String,
            verification: VerificationConfig,
        ): Kind? {
            if (!hasPermissions(verification.requiredPermissions)) return Kind.PERMISSION
            return when (verification.method) {
                // 앵커 바인딩 여부는 서버만 알아서(anchorsConfigured) 여기서는 확인하지 않는다.
                // 이미 등록했다면 상세 화면이 곧바로 다음 단계를 보여주므로 잘못된 안내는 아니다.
                // 장소를 피하는 방식(GPS_AVOID)도 어디를 피할지 먼저 찍어야 한다.
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
    }
