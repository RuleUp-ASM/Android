package com.ruleup.android_ruleup.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ruleup.domain.helper.PushNotificationHelper
import com.ruleup.domain.navigation.AppRoutes
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

// 앱 화면으로 직결되는 App Link 접두사. 커스텀 스킴(ruleup://)은 검증이 불가능해 폐기했다 —
// 임의 앱·웹페이지가 같은 형식으로 임의 화면에 진입시킬 수 있었기 때문이다.
private const val APP_LINK_PREFIX = "https://android.ruleup.co.kr/app/"

// 페이로드 명세("서버 FCM 푸시 페이로드 명세")의 type 딱지. 서버는 항상 데이터 전용 메시지를 보낸다.
private const val TYPE_NOTICE_CREATED = "NOTICE_CREATED"
private const val TYPE_SETUP_REQUIRED = "SETUP_REQUIRED"
private const val TYPE_PERMISSION_REQUIRED = "PERMISSION_REQUIRED"

/**
 * FCM 수신 진입점. 페이로드 명세의 type 기반으로 분기한다:
 * - [TYPE_NOTICE_CREATED]: 알림 표시(title=챌린지명, body=공지 제목) → 탭 시 공지 상세 진입
 * - 무음 쪽지(셋업/권한): 알림 없음 — 상태 재확인은 해당 화면 재진입 시 수행된다
 * - 미지 type: 조용히 폐기 (명세 규칙 3 — 서버·앱 독립 배포 보장)
 */
@AndroidEntryPoint
class RuleUpMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushNotificationHelper: PushNotificationHelper

    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    @Inject
    lateinit var observability: Observability

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            runCatching { pushTokenRegistrar.register(token) }
                .onFailure { observability.w(TAG, it) { "onNewToken 등록 실패 — 다음 앱 시작이 보정" } }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when (val type = data["type"]) {
            TYPE_NOTICE_CREATED -> showNoticeNotification(data)

            // 무음 쪽지(인증 스펙 관할): 알림을 띄우지 않는다. 셋업/권한 재확인은
            // 상세 화면 ON_RESUME 재조회가 이미 담당 — 백그라운드 선반영은 후속 고도화 여지.
            TYPE_SETUP_REQUIRED, TYPE_PERMISSION_REQUIRED -> Unit

            // 명세 규칙 3: 모르는 type 은 조용히 버린다.
            else -> observability.d(TAG) { "미지 푸시 type 무시: $type" }
        }
    }

    private fun showNoticeNotification(data: Map<String, String>) {
        val title = data["title"] ?: return
        val body = data["body"] ?: return
        val challengeId = data["challengeId"]
        val noticeId = data["noticeId"]
        // 서버 deepLink 문자열 형식에 의존하지 않고, ID 키로 앱 내부 주소 규칙(NavRouteUriParser)에
        // 맞춰 조립한다 — 페이로드 명세 §4 "앱 파서 규칙을 따르는 걸로 확정" 합의.
        val deepLink =
            if (challengeId != null && noticeId != null) {
                "$APP_LINK_PREFIX${AppRoutes.CHALLENGE_NOTICE_DETAIL}?challengeId=$challengeId&noticeId=$noticeId"
            } else {
                "$APP_LINK_PREFIX${AppRoutes.HOME}"
            }

        pushNotificationHelper.show(
            // 같은 챌린지의 공지 알림은 하나로 묶어 갱신한다 (명세 권장).
            id = (challengeId ?: deepLink).hashCode(),
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
