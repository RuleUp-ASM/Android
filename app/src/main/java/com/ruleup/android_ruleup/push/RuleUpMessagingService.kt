package com.ruleup.android_ruleup.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.d
import com.ruleup.observability.domain.api.w
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "[Push]"

// 페이로드 명세의 type 딱지.
private const val TYPE_SETUP_REQUIRED = "SETUP_REQUIRED"
private const val TYPE_PERMISSION_REQUIRED = "PERMISSION_REQUIRED"

/** FCM 수신 진입점. */
@AndroidEntryPoint
class RuleUpMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushTokenRegister: PushTokenRegister

    @Inject
    lateinit var observability: Observability

    @Inject
    lateinit var notificationPresenter: NotificationPresenter

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            runCatching { pushTokenRegister.register(token) }
                .onFailure { observability.w(TAG, it) { "onNewToken 등록 실패 — 다음 앱 시작이 보정" } }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when (val type = data["type"]) {
            // 무음 쪽지: 알림 없음.
            TYPE_SETUP_REQUIRED, TYPE_PERMISSION_REQUIRED -> Unit

            else -> {
                val push =
                    PushMessage.from(
                        data = data,
                        title = message.notification?.title ?: data["title"],
                        body = message.notification?.body ?: data["body"],
                    )
                if (push == null) {
                    observability.d(TAG) { "표시할 수 없는 푸시 무시: type=$type" }
                    return
                }
                notificationPresenter.show(push)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
