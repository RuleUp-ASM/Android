package com.ruleup.android_ruleup.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ruleup.domain.helper.PushNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "[Push]"

/**
 * FCM 수신 진입점. 서버 Push Outbox(공지 NOTICE_CREATED 등)가 보낸 메시지를
 * 시스템 알림으로 올리고, 탭 시 deepLink(ruleup://app/…)로 진입시킨다.
 *
 * 데이터 메시지 우선(title/body/deepLink 키), notification 페이로드는 폴백으로 읽는다 —
 * 백그라운드에서도 항상 이 경로를 타도록 서버는 데이터 메시지 발송이 기본이다.
 */
@AndroidEntryPoint
class RuleUpMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushNotificationHelper: PushNotificationHelper

    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            runCatching { pushTokenRegistrar.register(token) }
                .onFailure { Timber.tag(TAG).w(it, "onNewToken 등록 실패 — 다음 앱 시작이 보정") }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "RuleUp"
        val body = data["body"] ?: message.notification?.body ?: return
        // 딥링크가 없으면 홈으로 진입시킨다 (미등록 path 는 NavRouteUriParser 가 무시).
        val deepLink = data["deepLink"] ?: "ruleup://app/home"

        pushNotificationHelper.show(
            // 같은 딥링크(같은 공지) 재발송은 같은 알림을 갱신한다.
            id = deepLink.hashCode(),
            title = title,
            message = body,
            deepLink = deepLink,
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
