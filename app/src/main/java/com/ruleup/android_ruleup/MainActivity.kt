package com.ruleup.android_ruleup

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.metrics.performance.JankStats
import com.ruleup.android_ruleup.deeplink.resolveNewIntentRoute
import com.ruleup.android_ruleup.deeplink.resolveStartRoute
import com.ruleup.android_ruleup.deeplink.startStack
import com.ruleup.android_ruleup.observability.JankTracker
import com.ruleup.android_ruleup.observability.ScreenTracker
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.DeeplinkResolver
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.navigation.RouteAccessPolicy
import com.ruleup.domain.token.TokenRepository
import com.ruleup.logging.domain.BizLogger
import com.ruleup.observability.domain.api.Observability
import com.ruleup.onboarding.domain.account.AccountRestrictionProvider
import com.ruleup.onboarding.domain.navigation.SplashPage
import com.ruleup.profile.domain.navigation.AccountLockedPage
import com.ruleup.tti.domain.TtiRecorder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    lateinit var ttiRecorder: TtiRecorder

    @Inject
    lateinit var observability: Observability

    @Inject
    lateinit var bizLogger: BizLogger

    @Inject
    lateinit var jankTracker: JankTracker

    @Inject
    lateinit var pendingDeepLink: PendingDeepLink

    @Inject
    lateinit var tokenRepository: TokenRepository

    @Inject
    lateinit var deeplinkResolver: DeeplinkResolver

    @Inject
    lateinit var routeAccessPolicy: RouteAccessPolicy

    @Inject
    lateinit var accountRestrictionProvider: AccountRestrictionProvider

    @Inject
    lateinit var signupInviteStore: com.ruleup.onboarding.domain.auth.repository.SignupInviteStore

    private var jankStats: JankStats? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // @AndroidEntryPoint 의 필드 주입은 super.onCreate() 에서 일어난다.
        super.onCreate(savedInstanceState)
        // 딥링크는 인증보다 먼저 도착한다
        pendingDeepLink.set(resolveStartRoute(intent?.entryUri(), observability, deeplinkResolver))
        if (tokenRepository.cachedAccessToken() == null) intent?.entryUri()?.toString()?.let(signupInviteStore::capture)
        observeSessionEnd()
        val startStack = startStack()
        // 앱은 라이트 테마만 있다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            AppRoot(
                navigationHelper = navigationHelper,
                messageHelper = messageHelper,
                screenTracker = screenTracker,
                observability = observability,
                bizLogger = bizLogger,
                ttiRecorder = ttiRecorder,
                startStack = startStack,
            )
        }
        // 성능 채널로 집계해 내보낸다.
        jankStats = JankStats.createAndTrack(window, jankTracker::onFrame)
    }

    /** 세션이 끊기면 스플래시로 돌려보내 진입 판정을 다시 시킨다. */
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (tokenRepository.cachedAccessToken() == null) intent.entryUri()?.toString()?.let(signupInviteStore::capture)
        val route = intent.entryUri()?.let { resolveNewIntentRoute(it, observability, deeplinkResolver) } ?: return
        lifecycleScope.launch {
            // 로그인 화면 위에서 링크를 받으면 화면만 뜨고 API 가 401 을 받는다
            if (!tokenRepository.isLoggedIn.first() && routeAccessPolicy.requiresLogin(route.path)) {
                pendingDeepLink.set(route)
            } else {
                runCatching { accountRestrictionProvider.current() }
                    .onSuccess { restriction ->
                        if (restriction.isFullLock) {
                            navigationHelper.replaceStackWith(AccountLockedPage.toRoute())
                        } else {
                            navigationHelper.navigateByRoute(route)
                        }
                    }.onFailure { messageHelper.showToast("계정 상태를 확인하지 못했어요. 다시 시도해 주세요") }
            }
        }
    }
}

/** 외부 진입 목적지. */
internal fun Intent.entryUri(): Uri? = data ?: getStringExtra("deeplink")?.takeIf { it.isNotBlank() }?.let(Uri::parse)
