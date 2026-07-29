package com.ruleup.android_ruleup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.metrics.performance.JankStats
import com.ruleup.android_ruleup.deeplink.navRouteExtra
import com.ruleup.android_ruleup.deeplink.resolveNewIntentRoute
import com.ruleup.android_ruleup.deeplink.resolveStartRoute
import com.ruleup.android_ruleup.deeplink.startStack
import com.ruleup.android_ruleup.navigation.GenericNavKey
import com.ruleup.android_ruleup.observability.JankTracker
import com.ruleup.android_ruleup.observability.ScreenTracker
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.observability.domain.api.Observability
import com.ruleup.onboarding.domain.auth.SessionBootstrap
import com.ruleup.ui.navigation.PendingDeepLink
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigationHelper: NavigationHelper

    @Inject
    lateinit var messageHelper: MessageHelper

    @Inject
    lateinit var screenTracker: ScreenTracker

    @Inject
    lateinit var observability: Observability

    @Inject
    lateinit var jankTracker: JankTracker

    @Inject
    lateinit var pendingDeepLink: PendingDeepLink

    @Inject
    lateinit var sessionBootstrap: SessionBootstrap

    // 프레임 jank 측정. 창 단위로 묶어 성능 채널로 내보낸다([JankTracker]).
    private var jankStats: JankStats? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // @AndroidEntryPoint 의 필드 주입은 super.onCreate() 에서 일어난다.
        // 주입된 의존을 쓰는 코드는 반드시 그 뒤에 와야 한다.
        super.onCreate(savedInstanceState)
        // 딥링크는 인증보다 먼저 도착한다. 목적지는 보관만 하고, 스플래시가 자동 로그인을 마친 뒤
        // 꺼내 간다([PendingDeepLink]). 세션 없이 목적지를 띄우면 401 → 토큰 정리 → 로그인 화면으로
        // 튕기면서 딥링크가 유실된다.
        // 알림 탭은 extra 로, App Link 는 URI 로 온다. 알림이 먼저다 — 그쪽이 명시적 목적지다.
        pendingDeepLink.set(intent?.navRouteExtra() ?: resolveStartRoute(intent?.data, observability))
        // 컴포지션과 겹쳐 돌린다 — 자동 로그인은 토큰 재발급 네트워크 호출을 포함해 동기로 끝나지 않는다.
        sessionBootstrap.start()
        val startStack = startStack()
        // 시작 화면은 네비게이션 신호 없이 백스택으로 직접 세팅되므로 ScreenTracker 를 거치지 않는다.
        // 그대로 두면 스플래시의 ScreenView 가 누락되고, 그 구간 jank 가 'unknown' 에 귀속된다.
        (startStack.lastOrNull() as? GenericNavKey)?.let { screenTracker.onScreenEntered(it.path) }
        enableEdgeToEdge()
        setContent {
            AppRoot(
                navigationHelper = navigationHelper,
                messageHelper = messageHelper,
                screenTracker = screenTracker,
                observability = observability,
                startStack = startStack,
            )
        }
        // 성능 채널로 집계해 내보낸다. 릴리스에서도 켠다 — 실사용 기기의 jank 가 정작 필요한 데이터이고,
        // 프레임마다가 아니라 창 단위로 묶여 나가므로 이벤트 양이 통제된다.
        jankStats = JankStats.createAndTrack(window, jankTracker::onFrame)
    }

    // 화면이 보일 때만 측정(JankStats 권장).
    override fun onResume() {
        super.onResume()
        jankStats?.isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        jankStats?.isTrackingEnabled = false
    }

    // 앱이 떠 있는 동안 들어온 진입 처리. 알림 탭(extra)과 App Link(URI) 둘 다 받는다.
    // 해석할 수 없거나 미등록 path 는 무시된다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.navRouteExtra() ?: intent.data?.let { resolveNewIntentRoute(it, observability) }
        route?.let { navigationHelper.navigateByRoute(it) }
    }
}
