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

// 페이로드 명세("서버 FCM 푸시 페이로드 명세")의 type 딱지. 서버는 항상 데이터 전용 메시지를 보낸다.
private const val TYPE_SETUP_REQUIRED = "SETUP_REQUIRED"
private const val TYPE_PERMISSION_REQUIRED = "PERMISSION_REQUIRED"

/**
 * FCM 수신 진입점.
 *
 * **여기가 도는 건 포그라운드일 때뿐이다** — 백그라운드·종료 상태에서는 OS 가 payload 의
 * `notification` 블록을 자동 표시한다. 그래서 이 코드가 없다고 알림이 안 오는 게 아니라,
 * 앱을 보고 있을 때만 우리가 직접 그린다.
 *
 * 셋업·권한 쪽지는 여전히 무음이다 — 상태 재확인은 해당 화면 재진입이 담당한다.
 * `notification_id` 가 없는 메시지는 조용히 버린다(명세 규칙 3 — 서버·앱 독립 배포 보장).
 */
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
            // 무음 쪽지: 알림 없음. 셋업/권한 재확인은 상세 화면 ON_RESUME 재조회가 담당한다.
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
