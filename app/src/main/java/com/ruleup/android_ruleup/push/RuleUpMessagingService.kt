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
 * FCM 수신 진입점. 페이로드 명세의 type 기반으로 분기한다:
 * - 무음 쪽지(셋업/권한): 알림 없음 — 상태 재확인은 해당 화면 재진입 시 수행된다
 * - 미지 type: 조용히 폐기 (명세 규칙 3 — 서버·앱 독립 배포 보장)
 *
 * 공지가 제품에서 빠지면서 `NOTICE_CREATED` 분기가 사라졌고, 지금은 **알림을 띄우는 type 이 하나도
 * 없다.** 서버가 계속 보내더라도 미지 type 으로 조용히 버려진다.
 */
@AndroidEntryPoint
class RuleUpMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushTokenRegister: PushTokenRegister

    @Inject
    lateinit var observability: Observability

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
            // 무음 쪽지(인증 스펙 관할): 알림을 띄우지 않는다. 셋업/권한 재확인은
            // 상세 화면 ON_RESUME 재조회가 이미 담당 — 백그라운드 선반영은 후속 고도화 여지.
            TYPE_SETUP_REQUIRED, TYPE_PERMISSION_REQUIRED -> Unit

            // 명세 규칙 3: 모르는 type 은 조용히 버린다.
            else -> observability.d(TAG) { "미지 푸시 type 무시: $type" }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
