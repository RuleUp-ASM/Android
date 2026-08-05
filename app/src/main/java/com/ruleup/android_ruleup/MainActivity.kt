package com.ruleup.android_ruleup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.metrics.performance.JankStats
import com.ruleup.android_ruleup.deeplink.resolveNewIntentRoute
import com.ruleup.android_ruleup.deeplink.resolveStartRoute
import com.ruleup.android_ruleup.deeplink.startStack
import com.ruleup.android_ruleup.navigation.GenericNavKey
import com.ruleup.android_ruleup.observability.JankTracker
import com.ruleup.android_ruleup.observability.ScreenTracker
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.onboarding.domain.navigation.SplashPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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
    lateinit var tokenRepository: TokenRepository

    // 프레임 jank 측정. 창 단위로 묶어 성능 채널로 내보낸다([JankTracker]).
    private var jankStats: JankStats? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // @AndroidEntryPoint 의 필드 주입은 super.onCreate() 에서 일어난다.
        // 주입된 의존을 쓰는 코드는 반드시 그 뒤에 와야 한다.
        super.onCreate(savedInstanceState)
        // 딥링크는 인증보다 먼저 도착한다. 목적지는 보관만 하고, 스플래시가 자동 로그인을 마친 뒤
        // 꺼내 간다([PendingDeepLink]). 세션 없이 목적지를 띄우면 401 → 토큰 정리 → 로그인 화면으로
        // 튕기면서 딥링크가 유실된다.
        // 알림 탭도 App Link 도 URI 로 온다 — 해석은 한 갈래다.
        pendingDeepLink.set(resolveStartRoute(intent?.data, observability))
        observeSessionEnd()
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

    /**
     * 세션이 끊기면 스플래시로 돌려보내 진입 판정을 다시 시킨다.
     *
     * 이 경로가 필요한 건 **아무도 시작하지 않은 종료**가 있어서다 — 다른 기기 로그인이나
     * refreshToken 만료는 `TokenAuthenticator` 가 OkHttp 스레드에서 토큰을 지우는 것으로만
     * 드러난다. 거기엔 화면도 ViewModel 도 없고, `core:network` 가 네비게이션을 알 수도 없다.
     *
     * 첫 방출은 건너뛴다 — 앱 시작 시점의 로그인 여부는 전이가 아니다. 자동 로그인 실패도 토큰
     * 정리를 거치지만, 그때는 이미 스플래시가 떠 있어 [handleNavRoute] 가 아무것도 하지 않는다.
     */
    private fun observeSessionEnd() {
        lifecycleScope.launch {
            tokenRepository.isLoggedIn
                .distinctUntilChanged()
                .drop(1)
                .filter { loggedIn -> !loggedIn }
                .collect { navigationHelper.navigateTo(SplashPage) }
        }
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

    // 앱이 떠 있는 동안 들어온 진입 처리. 해석할 수 없거나 미등록 path 는 무시된다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data
            ?.let { resolveNewIntentRoute(it, observability) }
            ?.let { navigationHelper.navigateByRoute(it) }
    }
}
